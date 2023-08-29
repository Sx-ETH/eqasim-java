import pandas as pd
import sys
import numpy as np
import matplotlib.pyplot as plt

df_actual_path = sys.argv[1] # mikrocensus trips /cluster/work/ivt_vpl/kaghog/scenarios/switzerland_2020_scenario/calibration/mz_avg_trips_zurich_5km_within.csv
df_syn_path = sys.argv[2] # eqasim trips .csv from output /cluster/scratch/mfrancesc/IVT/SA_scenario/noDrt/eqasim_trips.csv
file_path = sys.argv[3]

# Define the distance bins (0 to 25000 meters with step of 1000 meters)
distance_bins = np.arange(0, 7500, 1000)

df_actual = pd.read_csv(df_actual_path, sep=";")
df_syn = pd.read_csv(df_syn_path, sep=";")

modes = ["bike", "car", "pt", "walk"]
df_actual = df_actual[df_actual["mode"].isin(modes)]
df_syn = df_syn[df_syn["mode"].isin(modes)]

df_syn = df_syn[~((df_syn["preceding_purpose"]=="outside") | 
         (df_syn["following_purpose"]=="outside"))]
    

print("mode shares synthetic: ")
print(df_syn["mode"].value_counts(normalize=True).sort_index())

print ("weighted mode shares mz: ")
print((df_actual.groupby(["mode"])["weight_person"].sum()/df_actual["weight_person"].sum()).sort_index())

def calculate_mode_share(df, distance_bins, weight_column=None):
    # Cut the dataframe into bins by distance
    df['distance_bin'] = pd.cut(df['euclidean_distance'], bins=distance_bins)

    if weight_column:
        mode_share = df.groupby(['distance_bin', 'mode'])[weight_column].sum() / df.groupby('distance_bin')[weight_column].sum()
    else:
        mode_share = df.groupby(['distance_bin', 'mode']).size() / df.groupby('distance_bin').size()

    return mode_share.unstack().fillna(0)


mode_share_reference = calculate_mode_share(df_actual, distance_bins, weight_column='weight_person')
mode_share_sim = calculate_mode_share(df_syn, distance_bins)

mode_colors = {'walk': 'blue', 'bike': 'orange', 'car': 'yellow', 'pt': 'red'}

#plot
plt.figure(figsize=(10, 6))
for mode in mode_share_reference.columns:
    plt.plot(distance_bins[:-1], mode_share_reference[mode], label=f'Reference {mode}', color=mode_colors[mode], linestyle='--')
    plt.plot(distance_bins[:-1], mode_share_sim[mode], label=f'Simulation {mode}', color=mode_colors[mode], linewidth=2)

plt.legend(loc="best")
plt.xlabel('Distance (m)')
plt.ylabel('Mode share')
plt.title('Mode share by distance')
plt.savefig(f"{file_path}/mode_share_distance_line.png")
plt.show()

##########################
#plot histogram
##########################
def add_small_hist(axes, r, c, act, x, y, bins, distance_column = "crowfly_distance", lab = ["Synthetic", "HTS"]):
    axes[r,c].hist(x, bins, alpha=0.5, label=lab[0], density=True)
    axes[r,c].hist(y[distance_column], bins, weights=y["weight_person"], alpha=0.5, label=lab[1], density=True)
    axes[r,c].set_ylabel("Percentage")
    axes[r,c].set_xlabel("Crowfly Distance [m]")
    axes[r,c].set_title(act.capitalize())
    axes[r,c].legend(loc="best")
    return axes
def plot_comparison_hist_mode(title, actual_df, synthetic_df, distance_column = "crowfly_distance", bins = np.linspace(0,25,120), dpi = 300, cols = 3, rows = 2):
    modelist = synthetic_df["mode"].unique()
    plt.rcParams['figure.dpi'] = dpi
    fig, axes = plt.subplots(nrows=rows, ncols=cols)
    idx=0
    for r in range(rows):
        for c in range(cols):
            x = synthetic_df[synthetic_df["mode"]==modelist[idx]][distance_column]
            y = actual_df[actual_df["mode"]==modelist[idx]][[distance_column, "weight_person"]]        
            axes = add_small_hist(axes, r, c, modelist[idx], x, y, bins, distance_column=distance_column)
            idx=idx+1
            if idx==5:
                break

    #fig.delaxes(axes[1,2])        
    fig.tight_layout()
    plt.savefig(f"{file_path}/{title}")
    plt.show()
    plt.close()

plot_comparison_hist_mode("mode_share_distance.png", df_actual, df_syn, bins = distance_bins, distance_column="euclidean_distance", cols = 2, rows = 2)
