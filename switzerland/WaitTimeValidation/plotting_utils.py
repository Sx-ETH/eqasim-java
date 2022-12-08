import geopandas as gpd
import pandas as pd
from tqdm import tqdm
import numpy as np
from sklearn.neighbors import KDTree
from matplotlib_scalebar.scalebar import ScaleBar
import seaborn as sns
import matplotlib.pyplot as plt
import contextily as cx


def get_lakes_gpd(lakes_path):
    df_lakes = gpd.read_file(lakes_path, geometry="geometry").to_crs("epsg:2056")
    return df_lakes

def get_zurich_districts_gpd(zurich_districts_path):
    df_zurich_districts = gpd.read_file(zurich_districts_path, encoding="latin1").to_crs("epsg:2056")
    df_zurich_districts = df_zurich_districts.rename({"knr": "district_id", "kname": "district_name"}, axis=1)
    df_zurich_districts = df_zurich_districts[["district_id", "district_name", "geometry"]].sort_values("district_id").reset_index(drop=True)

    return df_zurich_districts

#functions to input zones by points in a dataframe

def impute(df_points, df_zones, point_id_field, zone_id_field, fix_by_distance=False, chunk_size=10000,
           zone_type="", point_type=""):
    assert (type(df_points) == gpd.GeoDataFrame)
    assert (type(df_zones) == gpd.GeoDataFrame)

    assert (point_id_field in df_points.columns)
    assert (zone_id_field in df_zones.columns)
    assert (not zone_id_field in df_points.columns)

    df_original = df_points
    df_points = df_points[[point_id_field, "geometry"]]
    df_zones = df_zones[[zone_id_field, "geometry"]]

    print("Imputing %d %s zones onto %d %s points by spatial join..."
          % (len(df_zones), zone_type, len(df_points), point_type))

    result = []
    chunk_count = max(1, int(len(df_points) / chunk_size))
    for chunk in tqdm(np.array_split(df_points, chunk_count)):
        result.append(gpd.sjoin(df_zones, chunk, predicate="contains", how="right"))
    
    df_points = pd.concat(result).reset_index()

    if "left_index" in df_points: del df_points["left_index"]
    if "right_index" in df_points: del df_points["right_index"]

    invalid_mask = pd.isnull(df_points[zone_id_field])

    if fix_by_distance and np.any(invalid_mask):
        print("  Fixing %d points by centroid distance join..." % np.count_nonzero(invalid_mask))
        coordinates = np.vstack([df_zones["geometry"].centroid.x, df_zones["geometry"].centroid.y]).T
        kd_tree = KDTree(coordinates)

        df_missing = df_points[invalid_mask]
        coordinates = np.vstack([df_missing["geometry"].centroid.x, df_missing["geometry"].centroid.y]).T
        indices = kd_tree.query(coordinates, return_distance=False).flatten()

        df_points.loc[invalid_mask, zone_id_field] = df_zones.iloc[indices][zone_id_field].values

    return pd.merge(df_original, df_points[[point_id_field, zone_id_field]], on=point_id_field, how="left")

def get_metrics_for_zonal_plot(df, zones, zone_id, metrics=["waitTime"]):
    joined_df = pd.merge(df, zones, on=zone_id, how="right") #merge on right to ensure all zones are showing
    
    ## computing metrics for plot
    mean_metrics = joined_df.groupby(zone_id)[metrics].mean().fillna(0).reset_index()

    for col in metrics:
        if "time" in col.lower():
            mean_metrics[col] /= 60
    mean_metrics = mean_metrics.round(1)
    return mean_metrics


#add Zurich or zonal overlay
def zurich_overlay(ax, lakes_path, zurich_districts_path, lake_restriction, number_districts=True, district_col="knr"):
    # add lake
    df_lakes = get_lakes_gpd(lakes_path)
    f = df_lakes["geometry"].intersects(lake_restriction)
    df_lakes.loc[f, "geometry"].intersection(lake_restriction).plot(facecolor="skyblue", edgecolor="dodgerblue", ax=ax)

    # add districts with numbers
    df_districts = get_zurich_districts_gpd(zurich_districts_path)
    df_districts.plot(facecolor="none", edgecolor="black", ax=ax)
    if number_districts:
        for _, row in df_districts.iterrows():    
            ax.text(row["geometry"].centroid.x, row["geometry"].centroid.y, str(row[district_col]), color="black", fontsize=20, verticalalignment='center', horizontalalignment='center')
        
    return ax

def add_scalebar(ax):
    # add scale bar
    scalebar = ScaleBar(1, font_properties={'size': 20}, location="lower left", frameon=False, box_color=None)
    ax.add_artist(scalebar)
    scalebar.get_fixed_value()
    
    return ax

def add_north(ax):
    
   # add north arrow
    x_min = ax.get_xbound()[0]
    x_max = ax.get_xbound()[1]
    dx = x_max - x_min

    y_min = ax.get_ybound()[0]
    y_max = ax.get_ybound()[1]
    dy = y_max - y_min

    ax.text(x=x_min, y=y_min + dy*0.1, s='N', fontsize=30)
    ax.arrow(x=x_min + dx*0.017, y=y_min + dy*0.14, 
           dx=0, dy=0, width=0,
           length_includes_head=False,
           head_width=500, head_length=500, overhang=.2, facecolor='k', edgecolor=None, linewidth=0, 
           head_starts_at_zero=False)
    
    return ax

def plot_zonal_avg(metrics, zones, column, lake_restriction, lakes_path, zurich_districts_path, add_map=True):
    sns.set_context("poster")
    fig, ax = plt.subplots(1, 1, figsize=(13, 13))

    # allow to adjust colormap
    from mpl_toolkits.axes_grid1 import make_axes_locatable
    divider = make_axes_locatable(ax)
    cax = divider.new_vertical(size='5%', pad=0.6, pack_start = True)
    fig.add_axes(cax)

    temp = gpd.GeoDataFrame(pd.merge(metrics, zones))
    temp.plot(column, 
              ax=ax, 
             #  vmin=2.0,
             #  vmax=6.0,
              legend=True,
              legend_kwds=dict(label="Average " + column + " [min]",
                               orientation="horizontal",
                               shrink=0.7,
                               cax=cax
                               ),
              alpha= 0.7 if add_map else 1.0, 
              cmap='OrRd', 
              edgecolor="grey",
              )
    ax = zurich_overlay(ax,lakes_path, zurich_districts_path, lake_restriction,number_districts=True, district_col="district_id")
    ax = add_scalebar(ax)

    # add north arrow
    ax = add_north(ax)
    if add_map:
        cx.add_basemap(ax, crs=temp.crs)
    ax.set_axis_off()

def plot_column_by_trip_density_scatter(drt_legs_with_zone_id, zone_id_field, column):
    sns.set_context('notebook')
    grouped_by_n_trips = drt_legs_with_zone_id.groupby(zone_id_field) \
                            .agg(columnAvg=(column, 'mean'), nTrips=('trip_id','size'))
    x = grouped_by_n_trips.nTrips
    y = grouped_by_n_trips.columnAvg
    sns.regplot(x=x,y=y)