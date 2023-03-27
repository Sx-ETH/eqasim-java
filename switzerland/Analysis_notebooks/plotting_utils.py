import os
os.environ['USE_PYGEOS'] = '0'
import geopandas as gpd
import pandas as pd
from tqdm import tqdm
import numpy as np
from sklearn.neighbors import KDTree
from matplotlib_scalebar.scalebar import ScaleBar
import seaborn as sns
import matplotlib.pyplot as plt
import contextily as cx
from IPython.display import Image
from shapely.geometry import LineString, Point
import shapely
import utils



def get_lakes_gpd(lakes_path):
    df_lakes = gpd.read_file(lakes_path, geometry="geometry").to_crs("epsg:2056")
    return df_lakes

def get_zurich_districts_gpd(zurich_districts_path):
    df_zurich_districts = gpd.read_file(zurich_districts_path, encoding='latin1').to_crs("epsg:2056")
    df_zurich_districts = df_zurich_districts.rename({"knr": "district_id", "kname": "district_name"}, axis=1)
    df_zurich_districts = df_zurich_districts[["district_id", "district_name", "geometry"]].sort_values("district_id").reset_index(drop=True)

    return df_zurich_districts

# Functions to plot the average by zone

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

def plot_zonal_avg(metrics, zones, column, lake_restriction, lakes_path, zurich_districts_path, add_map=True, in_subplot=False):
    sns.set_context("poster")
    if not in_subplot:
        plt.figure(figsize=(13,13))
    fig = plt.gcf()
    ax = plt.gca()

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

    
# Plots the wait time by districts
def plot_districts_wait_time(it_drt_trips_stats, lake_path, zurich_districts_path):
    it_drt_trips_stats_gpd = utils.convert_drt_legs_to_gpd(it_drt_trips_stats)
    df_districts = get_zurich_districts_gpd(zurich_districts_path)
    imputed_from_legs = impute(it_drt_trips_stats_gpd, df_districts, "trip_id", "district_id",fix_by_distance=False).drop("geometry", axis=1)
    #Since we are not fixing by distance, checking how many points are outside the districts
    print("no. of trips outside the district: ", sum(pd.isna(imputed_from_legs["district_id"])))

    district_metrics_drt_legs = get_metrics_for_zonal_plot(imputed_from_legs, df_districts, "district_id", metrics=["waitTime"])#, "delayFactor", "delayFactor_estimated"])
    plot_zonal_avg(district_metrics_drt_legs, df_districts, 
                              'waitTime', shapely.ops.unary_union([geo for geo in df_districts["geometry"]]),
                             lake_path, zurich_districts_path, add_map=True)

# Plots the wait time in different grid sizes
def plot_multigrid_wait_time(grid_sizes, it_drt_trips_stats, zurich_shp_path, lake_path, zurich_districts_path, map_limit=None):
    it_drt_trips_stats_gpd = utils.convert_drt_legs_to_gpd(it_drt_trips_stats)
    zurich_shp = gpd.read_file(zurich_shp_path)
    n_sizes = len(grid_sizes)
    plt.figure(figsize=(13,n_sizes*25))
    for idx,gs in enumerate(grid_sizes,start=0):
        plt.subplot(n_sizes*2,1,idx*2+1)
        grid = utils.create_grid_from_shapefile(zurich_shp_path, gs)
        lake_limit = zurich_shp.loc[0].geometry
        if map_limit:
            grid = gpd.clip(grid, map_limit)
            lake_limit = map_limit
        imputed = impute(it_drt_trips_stats_gpd, grid, "trip_id", "grid_id",fix_by_distance=False).drop("geometry", axis=1)
        metrics = get_metrics_for_zonal_plot(imputed, grid, "grid_id", metrics=["waitTime"])
        plot_zonal_avg(metrics, grid, 'waitTime', 
                              lake_limit, lake_path, zurich_districts_path, add_map=True, in_subplot=True)
        plt.title('grid size = ' + str(gs) + 'm')
        
        plt.subplot(n_sizes*2,1,idx*2+2)
        sns.set_context('notebook')
        grouped_by_n_trips = imputed.groupby('grid_id') \
                                .agg(columnAvg=('waitTime', 'mean'), nTrips=('trip_id','size'))
        #if limit_n_trips:
        #    grouped_by_n_trips = grouped_by_n_trips[grouped_by_n_trips.nTrips < limit_n_trips]
        x = grouped_by_n_trips.nTrips
        y = grouped_by_n_trips.columnAvg / 60
        sns.regplot(x=x,y=y)
        plt.ylabel('Waiting time (min)')
        
        #plotting_utils.plot_column_by_trip_density_scatter(imputed_from_trips_stats, 'district_id', 'waitTime')
    plt.tight_layout()
    plt.show()

def plot_column_by_trip_density_scatter(drt_legs_with_zone_id, zone_id_field, column, limit_n_trips = None):
    sns.set_context('notebook')
    grouped_by_n_trips = drt_legs_with_zone_id.groupby(zone_id_field) \
                            .agg(columnAvg=(column, 'mean'), nTrips=('trip_id','size'))
    if limit_n_trips:
        grouped_by_n_trips = grouped_by_n_trips[grouped_by_n_trips.nTrips < limit_n_trips]
    x = grouped_by_n_trips.nTrips
    y = grouped_by_n_trips.columnAvg
    sns.regplot(x=x,y=y)

# Plot the delayFactor heatmap binning by origin and destination (district-level)
# Because if we do it on grid level we would have to few data points and a too big matrix    
def plot_OD_delayFactor_heatmaps(it_drt_trips_stats, zurich_districts_path):
    it_drt_trips_stats_gpd = utils.convert_drt_legs_to_gpd(it_drt_trips_stats)
    df_districts = get_zurich_districts_gpd(zurich_districts_path)
    imputed_from_trips_stats = impute(it_drt_trips_stats_gpd, df_districts, "trip_id", "district_id",fix_by_distance=False).drop("geometry", axis=1)
    imputed_from_trips_stats['geometry'] = imputed_from_trips_stats['destination_geometry']
    imputed_from_trips_stats = imputed_from_trips_stats.rename(columns={'district_id': 'origin_district_id'})
    imputed_from_trips_stats = gpd.GeoDataFrame(imputed_from_trips_stats)
    imputed_from_trips_stats.crs = "epsg:2056"
    imputed_from_trips_stats = impute(imputed_from_trips_stats, df_districts, "trip_id", "district_id",fix_by_distance=False).drop("geometry", axis=1)
    imputed_from_trips_stats = imputed_from_trips_stats.rename(columns={'district_id': 'destination_district_id'})
    
    
    filtered_imputed = imputed_from_trips_stats[imputed_from_trips_stats.routerUnsharedTime != 0].copy()
    group_by = filtered_imputed.groupby(['origin_district_id', 'destination_district_id'])
    average_df_router = group_by.delayFactor.mean().unstack()
    average_df_estimated = group_by.delayFactorEstimatedDRT.mean().unstack()
    computed_df_router = (group_by.totalTravelTime.sum() / group_by.routerUnsharedTime.sum()).unstack()
    computed_df_estimated = (group_by.totalTravelTime.sum() / group_by.estimatedUnsharedTime.sum()).unstack()
    
    to_plot = [average_df_router, average_df_estimated, computed_df_router, computed_df_estimated]
    titles = ['Avg of DF using router', 'Avg of DF using estimation from DRT', 'Computed DF from sum using router', 'Computed DF from sum estimation from DRT']
    
    fig = plt.figure(figsize=(20,20))
    
    for idx, data in enumerate(to_plot, start=1):
        ax = plt.subplot(2,2,idx)
        im = ax.matshow(data.values)

    # Show all ticks and label them with the respective list entries
        #display(data)
        plt.xticks(np.arange(len(data.columns)), labels=data.columns.values.astype('int'))
        plt.yticks(np.arange(len(data.index)), labels=data.index.values.astype('int'))
        plt.xlabel(data.columns.name)
        plt.ylabel(data.index.name)
        ax.xaxis.set_ticks_position('bottom')
        plt.title(titles[idx-1])
        cbar = ax.figure.colorbar(im, ax=ax)
        cbar.ax.set_ylabel('delayFactor', rotation=-90, va="bottom")
    plt.tight_layout()
    plt.show()

    
def show_modeshare(folder):
    return Image(folder + '/modestats.png')
    
def show_occupancy_profile(folder, iteration=-1):
    if iteration == -1:
        iteration = len(os.listdir(folder + '/ITERS')) - 1
    return Image(folder + '/ITERS/it.' + str(iteration) + '/' + str(iteration) + '.occupancy_time_profiles_Line_drt.png')

# Plots the euclidean distance distribution of the DRT trips, and displays a description of the stats
def plot_euclidean_distance_ditribution(data, iteration=-1):
    it_drt_trips_stats = data['drt_trips_stats'][iteration]
    euclidean_distance = np.sqrt((it_drt_trips_stats.startX -it_drt_trips_stats.endX)**2 + (it_drt_trips_stats.startY -it_drt_trips_stats.endY)**2)
    display(euclidean_distance.describe(percentiles=[.25,.50,.75,.90,.95,.99]).to_frame().transpose())
    plt.hist(euclidean_distance / 1000, range=(0,10), density=True)
    plt.xlabel('Euclidean distance (km)')
    plt.ylabel('Number of DRT trips')
    plt.show()

    
def avg_by_time_bin(drt_trips_stats, column, start_time=6, end_time=24, bin_duration_min=30):
    legs = drt_trips_stats.copy()
    n_bins = (end_time - start_time) * 60 // bin_duration_min
    bins = [start_time*3600 + i*bin_duration_min*60 for i in range(0,n_bins+1)]
    legs['time_bin'] = pd.cut(legs.startTime, bins).map(lambda x: int((x.left + x.right)/2))
    if column == 'compute_total_delay_factor_estimated':
        grouped = legs.groupby(['time_bin'])['totalTravelTime'].sum() / legs.groupby(['time_bin'])['estimatedUnsharedTime'].sum()
    elif column == 'compute_total_delay_factor_router':
        grouped = legs.groupby(['time_bin'])['totalTravelTime'].sum() / legs.groupby(['time_bin'])['routerUnsharedTime'].sum()
    else:
        grouped = legs.groupby(['time_bin'])[column].mean()
    return grouped

def median_by_time_bin(drt_trips_stats, column, start_time=6, end_time=24, bin_duration_min=30):
    legs = drt_trips_stats.copy()
    n_bins = (end_time - start_time) * 60 // bin_duration_min
    bins = [start_time*3600 + i*bin_duration_min*60 for i in range(0,n_bins+1)]
    legs['time_bin'] = pd.cut(legs.startTime, bins).map(lambda x: int((x.left + x.right)/2))
    grouped = legs.groupby(['time_bin'])[column].median()
    return grouped

def avg_by_euclidean_distance_bin(drt_trips_stats, column, min_distance=0, max_distance=5000, bin_distance_m=200):
    legs = drt_trips_stats.copy()
    n_bins = (max_distance - min_distance) // bin_distance_m
    distance_bins = [min_distance + i*bin_distance_m for i in range(n_bins + 1)]
    legs['euclidean_distance'] = np.sqrt((legs.startX - legs.endX)**2 + (legs.startY - legs.endY)**2)
    legs['distance_bin'] = pd.cut(legs.euclidean_distance, distance_bins).map(lambda x: int((x.left + x.right)/2))
    if column == 'compute_total_delay_factor_estimated':
        grouped = legs.groupby(['distance_bin'])['totalTravelTime'].sum() / legs.groupby(['distance_bin'])['estimatedUnsharedTime'].sum()
    elif column == 'compute_total_delay_factor_router':
        grouped = legs.groupby(['distance_bin'])['totalTravelTime'].sum() / legs.groupby(['distance_bin'])['routerUnsharedTime'].sum()
    else:
        grouped = legs.groupby(['distance_bin'])[column].mean()
    return grouped


def plot_delay_factor(data, start_time, end_time, bin_duration_min, min_distance, max_distance, bin_distance_m, iteration=-1, plot_estimated=True):
    it_drt_trip_stats = data['drt_trips_stats'][iteration].copy(deep=True)
    filtered_without_router_zeros = it_drt_trip_stats[it_drt_trip_stats.routerUnsharedTime != 0]
    delayFactor_avg = avg_by_time_bin(filtered_without_router_zeros, 'delayFactor', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
    delayFactorEstimatedDRT_avg = avg_by_time_bin(filtered_without_router_zeros, 'delayFactorEstimatedDRT', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
    delayFactorComputedRouter_avg = avg_by_time_bin(filtered_without_router_zeros, 'compute_total_delay_factor_router', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
    delayFactorComputedEstimatedDRT_avg = avg_by_time_bin(filtered_without_router_zeros, 'compute_total_delay_factor_estimated', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
    plt.figure(figsize=(15,15))
    
    plt.subplot(2,2,1)
    xticks = [z*3600 for z in range(start_time, end_time+1, 2)]
    xticks_labels = [str(z) + 'h' for z in range(start_time, end_time+1, 2)]
    
    plt.plot(delayFactor_avg.index.values, delayFactor_avg.values, 'ro-', label='Avg of delay factor using router')
    plt.plot(delayFactorComputedRouter_avg.index.values, delayFactorComputedRouter_avg.values, 'ro--', label='Computed from sum delay factor using router')

    if plot_estimated:
        plt.plot(delayFactorEstimatedDRT_avg.index.values, delayFactorEstimatedDRT_avg.values, 'bo-', label='Avg of delay factor using estimated from DRT')
        plt.plot(delayFactorComputedEstimatedDRT_avg.index.values, delayFactorComputedEstimatedDRT_avg.values, 'bo--', label='Computed from sum delay factor using estimated from DRT')
    plt.legend()
    plt.xlim(start_time*3600,end_time*3600)
    plt.xticks(xticks, xticks_labels)
    plt.title('Delay Factor by departure time\n (filtering the trips with 0 predicted time by the router)')
    plt.ylabel('Delay Factor')
    plt.xlabel('Time of the day')

    
    plt.subplot(2,2,3)
    
    delayFactorEstimatedDRT_avg = avg_by_time_bin(it_drt_trip_stats, 'delayFactorEstimatedDRT', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
    delayFactorComputedRouter_avg = avg_by_time_bin(it_drt_trip_stats, 'compute_total_delay_factor_router', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
    delayFactorComputedEstimatedDRT_avg = avg_by_time_bin(it_drt_trip_stats, 'compute_total_delay_factor_estimated', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
    
    plt.plot(delayFactorComputedRouter_avg.index.values, delayFactorComputedRouter_avg.values, 'ro--', label='Computed from sum delay factor using router')
    if plot_estimated:
        plt.plot(delayFactorEstimatedDRT_avg.index.values, delayFactorEstimatedDRT_avg.values, 'bo-', label='Avg of delay factor using estimated from DRT')
        plt.plot(delayFactorComputedEstimatedDRT_avg.index.values, delayFactorComputedEstimatedDRT_avg.values, 'bo--', label='Computed from sum delay factor using estimated from DRT')

    plt.legend()
    plt.xlim(start_time*3600,end_time*3600)
    plt.xticks(xticks, xticks_labels)
    plt.title('Delay Factor by departure time\n (without filtering)')
    plt.ylabel('Delay Factor')
    plt.xlabel('Time of the day')

    
    
    plt.subplot(2,2,2)
    
    delayFactor_avg = avg_by_euclidean_distance_bin(filtered_without_router_zeros, 'delayFactor', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)
    delayFactorEstimatedDRT_avg = avg_by_euclidean_distance_bin(filtered_without_router_zeros, 'delayFactorEstimatedDRT', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)
    delayFactorComputedRouter_avg = avg_by_euclidean_distance_bin(filtered_without_router_zeros, 'compute_total_delay_factor_router', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)
    delayFactorComputedEstimatedDRT_avg = avg_by_euclidean_distance_bin(filtered_without_router_zeros, 'compute_total_delay_factor_estimated',min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)
    plt.plot(np.array(delayFactor_avg.index.values)/1000, delayFactor_avg.values, 'ro-', label='Avg of delay factor using router')
    plt.plot(np.array(delayFactorComputedRouter_avg.index.values)/1000, delayFactorComputedRouter_avg.values, 'ro--', label='Computed from sum delay factor using router')
    if plot_estimated:
        plt.plot(np.array(delayFactorEstimatedDRT_avg.index.values)/1000, delayFactorEstimatedDRT_avg.values, 'bo-', label='Avg of delay factor using estimated from DRT')
        plt.plot(np.array(delayFactorComputedEstimatedDRT_avg.index.values)/1000, delayFactorComputedEstimatedDRT_avg.values, 'bo--', label='Computed from sum delay factor using estimated from DRT')

    plt.legend()
    plt.title('Delay Factor by euclidean distance\n (filtering the trips with 0 predicted time by the router)')
    plt.xlabel('Euclidean distance (km)')
    plt.ylabel('Delay Factor')
    
    plt.subplot(2,2,4)
    
    delayFactor_avg = avg_by_euclidean_distance_bin(it_drt_trip_stats, 'delayFactor', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)
    delayFactorEstimatedDRT_avg = avg_by_euclidean_distance_bin(it_drt_trip_stats, 'delayFactorEstimatedDRT', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)
    delayFactorComputedRouter_avg = avg_by_euclidean_distance_bin(it_drt_trip_stats, 'compute_total_delay_factor_router', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)
    delayFactorComputedEstimatedDRT_avg = avg_by_euclidean_distance_bin(it_drt_trip_stats, 'compute_total_delay_factor_estimated',min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)
    
    plt.plot(np.array(delayFactorComputedRouter_avg.index.values)/1000, delayFactorComputedRouter_avg.values, 'ro--', label='Computed from sum delay factor using router')
    if plot_estimated:
        # TODO: Ask if we should plot this one (here we can see that only trips with low distance have 0 prediction)
        plt.plot(np.array(delayFactorEstimatedDRT_avg.index.values)/1000, delayFactorEstimatedDRT_avg.values, 'bo-', label='Avg of delay factor using estimated from DRT')
        plt.plot(np.array(delayFactorComputedEstimatedDRT_avg.index.values)/1000, delayFactorComputedEstimatedDRT_avg.values, 'bo--', label='Computed from sum delay factor using estimated from DRT')

    
    plt.legend()
    plt.title('Delay Factor by euclidean distance\n (without filtering)')
    plt.xlabel('Euclidean distance (km)')
    plt.ylabel('Delay Factor')
    plt.tight_layout()
    plt.show()
    

def plot_df_multiple_time_bins(data, start_time, end_time, bin_durations_min, iteration=-1):
    it_drt_trip_stats = data['drt_trips_stats'][iteration]
    filtered_without_router_zeros = it_drt_trip_stats[it_drt_trip_stats.routerUnsharedTime != 0]
    n_rows = (len(bin_durations_min) - 1) // 2 + 1
    plt.figure(figsize=(15, n_rows * 7.5))
    
    xticks = [z*3600 for z in range(start_time, end_time+1, 2)]
    xticks_labels = [str(z) + 'h' for z in range(start_time, end_time+1, 2)]
    
    for idx,time_bin in enumerate(bin_durations_min, start=1):
        delayFactor_avg = avg_by_time_bin(filtered_without_router_zeros, 'delayFactor', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=time_bin)
        delayFactorEstimatedDRT_avg = avg_by_time_bin(filtered_without_router_zeros, 'delayFactorEstimatedDRT', start_time=start_time, 
                                         end_time=end_time, bin_duration_min=time_bin)
        delayFactorComputedRouter_avg = avg_by_time_bin(filtered_without_router_zeros, 'compute_total_delay_factor_router', start_time=start_time, 
                                         end_time=end_time, bin_duration_min=time_bin)
        delayFactorComputedEstimatedDRT_avg = avg_by_time_bin(filtered_without_router_zeros, 'compute_total_delay_factor_estimated', start_time=start_time, 
                                         end_time=end_time, bin_duration_min=time_bin)
    
        plt.subplot(n_rows,2,idx)
        plt.plot(delayFactor_avg.index.values, delayFactor_avg.values, 'ro-', label='Avg of delay factor using router')
        plt.plot(delayFactorEstimatedDRT_avg.index.values, delayFactorEstimatedDRT_avg.values, 'bo-', label='Avg of delay factor using estimated from DRT')
        plt.plot(delayFactorComputedRouter_avg.index.values, delayFactorComputedRouter_avg.values, 'ro--', label='Computed from sum delay factor using router')
        plt.plot(delayFactorComputedEstimatedDRT_avg.index.values, delayFactorComputedEstimatedDRT_avg.values, 'bo--', label='Computed from sum delay factor using estimated from DRT')

        #plt.plot(delayFactor_avg.index.values, delayFactor_avg.values, 'o-', label=str(time_bin) + 'min')
        plt.legend()
        plt.xlim(start_time*3600,end_time*3600)
        plt.xticks(xticks, xticks_labels)
        plt.title('Delay Factor by departure time\n (filtering the trips with 0 predicted time by the router)\nTime bin = ' + str(time_bin) + ' min')
        plt.ylabel('Delay Factor')
        plt.xlabel('Time of the day')
    
    plt.tight_layout()
    plt.show()
        
def plot_df_multiple_distance_bins(data,  min_distance, max_distance, bin_distances_m, iteration=-1):
    it_drt_trip_stats = data['drt_trips_stats'][iteration]
    filtered_without_router_zeros = it_drt_trip_stats[it_drt_trip_stats.routerUnsharedTime != 0]
    n_rows = (len(bin_distances_m) - 1) // 2 + 1
    plt.figure(figsize=(15, n_rows * 7.5))
    
    for idx,distance_bin in enumerate(bin_distances_m, start=1):
        delayFactor_avg = avg_by_euclidean_distance_bin(filtered_without_router_zeros, 'delayFactor', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=distance_bin)
        delayFactorEstimatedDRT_avg = avg_by_euclidean_distance_bin(filtered_without_router_zeros, 'delayFactorEstimatedDRT', min_distance=min_distance, 
                                         max_distance=max_distance, bin_distance_m=distance_bin)
        delayFactorComputedRouter_avg = avg_by_euclidean_distance_bin(filtered_without_router_zeros, 'compute_total_delay_factor_router', min_distance=min_distance, 
                                         max_distance=max_distance, bin_distance_m=distance_bin)
        delayFactorComputedEstimatedDRT_avg = avg_by_euclidean_distance_bin(filtered_without_router_zeros, 'compute_total_delay_factor_estimated',min_distance=min_distance, 
                                         max_distance=max_distance, bin_distance_m=distance_bin)
        plt.subplot(n_rows,2,idx)
        
        plt.plot(np.array(delayFactor_avg.index.values)/1000, delayFactor_avg.values, 'ro-', label='Avg of delay factor using router')
        plt.plot(np.array(delayFactorEstimatedDRT_avg.index.values)/1000, delayFactorEstimatedDRT_avg.values, 'bo-', label='Avg of delay factor using estimated from DRT')
        plt.plot(np.array(delayFactorComputedRouter_avg.index.values)/1000, delayFactorComputedRouter_avg.values, 'ro--', label='Computed from sum delay factor using router')
        plt.plot(np.array(delayFactorComputedEstimatedDRT_avg.index.values)/1000, delayFactorComputedEstimatedDRT_avg.values, 'bo--', label='Computed from sum delay factor using estimated from DRT')
        plt.legend()
        plt.title('Delay Factor by euclidean distance\n (filtering the trips with 0 predicted time by the router)\nDistance bin = ' + str(distance_bin) + 'm')
        plt.xlabel('Euclidean distance (km)')
        plt.ylabel('Delay Factor')
    
    plt.tight_layout()
    plt.show()

    
def plot_waiting_time(data, start_time, end_time, bin_duration_min, min_distance, max_distance, bin_distance_m, iteration=-1):
    it_drt_trip_stats = data['drt_trips_stats'][iteration]
    filtered_without_router_zeros = it_drt_trip_stats[it_drt_trip_stats.routerUnsharedTime != 0]
    
    waitTime_avg = avg_by_time_bin(filtered_without_router_zeros, 'waitTime', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
    
    waitTime_median = median_by_time_bin(filtered_without_router_zeros, 'waitTime', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
    
    plt.figure(figsize=(15,15))
    
    plt.subplot(2,2,1)
    xticks = [z*3600 for z in range(start_time, end_time+1, 2)]
    xticks_labels = [str(z) + 'h' for z in range(start_time, end_time+1, 2)]
    
    plt.plot(waitTime_avg.index.values, waitTime_avg.values/60, 'o-', label='Avg of wait time')
    plt.plot(waitTime_median.index.values, waitTime_median.values/60, 'o-', label='Median of wait time')
    plt.xlim(start_time*3600,end_time*3600)
    plt.xticks(xticks, xticks_labels)
    plt.title('Wait time by departure time\n (filtering the trips with 0 predicted time by the router)')
    plt.ylabel('Wait time (min)')
    plt.xlabel('Time of the day')
    ax = plt.gca()
    ax.axhline(y=filtered_without_router_zeros.waitTime.mean()/60, color="black", label='Mean of all times')
    ax.axhline(y=filtered_without_router_zeros.waitTime.median()/60, color="black",ls = '--', label='Median of all times')
    plt.legend()
    
    plt.subplot(2,2,3)
    waitTime_avg = avg_by_time_bin(it_drt_trip_stats, 'waitTime', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
    waitTime_median = median_by_time_bin(it_drt_trip_stats, 'waitTime', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
    plt.plot(waitTime_avg.index.values, waitTime_avg.values/60, 'o-', label='Avg of wait time')
    plt.plot(waitTime_median.index.values, waitTime_median.values/60, 'o-', label='Median of wait time')
    plt.xlim(start_time*3600,end_time*3600)
    plt.xticks(xticks, xticks_labels)
    plt.title('Wait time by departure time\n (without filtering)')
    plt.ylabel('Wait time (min)')
    plt.xlabel('Time of the day')
    ax = plt.gca()
    ax.axhline(y=it_drt_trip_stats.waitTime.mean()/60, color="black", label='Mean of all times')
    ax.axhline(y=it_drt_trip_stats.waitTime.median()/60, color="black",ls = '--', label='Median of all times')
    plt.legend()
    
    plt.subplot(2,2,2)
    waitTime_avg = avg_by_euclidean_distance_bin(filtered_without_router_zeros, 'waitTime', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)
    plt.plot(np.array(waitTime_avg.index.values) / 1000, waitTime_avg.values/60, 'o-', label='Avg of wait time')
    plt.title('Wait time by euclidean distance\n (filtering the trips with 0 predicted time by the router)')
    plt.xlabel('Euclidean distance (km)')
    plt.legend()
    plt.subplot(2,2,4)
    waitTime_avg = avg_by_euclidean_distance_bin(it_drt_trip_stats, 'waitTime', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)
    plt.plot(np.array(waitTime_avg.index.values) / 1000, waitTime_avg.values/60, 'o-', label='Avg of wait time')
    plt.title('Wait time by euclidean distance\n (without filtering)')
    plt.ylabel('Wait time (min)')
    plt.xlabel('Euclidean distance (km)')
    plt.legend()
    
    plt.show()
 
def plot_waiting_time_multiple_time_bins(data, start_time, end_time, bin_durations_min, iteration=-1):
    it_drt_trip_stats = data['drt_trips_stats'][iteration]
    filtered_without_router_zeros = it_drt_trip_stats[it_drt_trip_stats.routerUnsharedTime != 0]
    n_rows = (len(bin_durations_min) - 1) // 2 + 1
    plt.figure(figsize=(15, n_rows * 7.5))
    
    xticks = [z*3600 for z in range(start_time, end_time+1, 2)]
    xticks_labels = [str(z) + 'h' for z in range(start_time, end_time+1, 2)]
    
    for idx,time_bin in enumerate(bin_durations_min, start=1):
        waitTime_avg = avg_by_time_bin(filtered_without_router_zeros, 'waitTime', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=time_bin)
        
        plt.subplot(n_rows,2,idx)
        plt.plot(waitTime_avg.index.values, waitTime_avg.values/60, 'o-', label='Avg of wait time')        
        plt.xlim(start_time*3600,end_time*3600)
        plt.xticks(xticks, xticks_labels)
        plt.title('Wait time by departure time\n (filtering the trips with 0 predicted time by the router)\nTime bin = ' + str(time_bin) + ' min')
        plt.ylabel('Wait time (min)')
        plt.xlabel('Time of the day')
    
    plt.tight_layout()
    plt.show()

def plot_waiting_time_multiple_distance_bins(data,  min_distance, max_distance, bin_distances_m, iteration=-1):
    it_drt_trip_stats = data['drt_trips_stats'][iteration]
    filtered_without_router_zeros = it_drt_trip_stats[it_drt_trip_stats.routerUnsharedTime != 0]
    n_rows = (len(bin_distances_m) - 1) // 2 + 1
    plt.figure(figsize=(15, n_rows * 7.5))
    
    for idx,distance_bin in enumerate(bin_distances_m, start=1):
        plt.subplot(n_rows,2,idx)
        waitTime_avg = avg_by_euclidean_distance_bin(filtered_without_router_zeros, 'waitTime', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=distance_bin)
        plt.plot(np.array(waitTime_avg.index.values) / 1000, waitTime_avg.values/60, 'o-', label='Avg of wait time')
        plt.title('Wait time by euclidean distance\n (filtering the trips with 0 predicted time by the router)\nDistance bin = ' + str(distance_bin) + 'm')
        plt.xlabel('Euclidean distance (km)')
    
    plt.tight_layout()
    plt.show()
    

def plot_difference_estimated_router(data, iteration=-1):
    it_drt_trip_stats = data['drt_trips_stats'][iteration]
    filtered_without_router_zeros = it_drt_trip_stats[it_drt_trip_stats.routerUnsharedTime != 0]
    abs_difference = np.abs(filtered_without_router_zeros['estimatedUnsharedTime'] - filtered_without_router_zeros['routerUnsharedTime'])
    abs_difference_min = abs_difference / 60 #absolute difference in minutes
    rel_difference = abs_difference / filtered_without_router_zeros['estimatedUnsharedTime']
    print('Description of abs difference in minutes')
    display(abs_difference_min.describe(percentiles=[0.25,0.5,0.75,0.9,0.95,0.99,0.999]).to_frame().transpose())
    print('Description of relative difference')
    display(rel_difference.describe(percentiles=[0.25,0.5,0.75,0.9,0.95,0.99,0.999]).to_frame().transpose())
    plt.figure(figsize=(15,20))
    plt.subplot(2,2,1)
    plt.hist(abs_difference_min, bins=100, range=(0,15), density=True)
    plt.title('Absolute difference')
    plt.xlabel('difference (min)')
    plt.subplot(2,2,2)
    plt.hist(rel_difference, bins=100, range=(0,1), density=True)
    plt.title('Relative difference')
    plt.subplot(2,2,3)
    plt.boxplot(abs_difference_min, positions=[1])
    plt.ylim(-1,15)
    ax2 = plt.gca().twinx()
    ax2.boxplot(abs_difference_min, positions=[2])
    plt.xticks([1,2],['Zoomed','Non zoomed'])
    plt.subplot(2,2,4)
    plt.boxplot(rel_difference, positions=[1])
    plt.ylim(-0.1,1)
    ax2 = plt.gca().twinx()
    ax2.boxplot(rel_difference, positions=[2])
    plt.xticks([1,2],['Zoomed','Non zoomed'])
    plt.tight_layout()
    plt.show()

def show_top_differences(data, iteration=-1, n=20):
    it_drt_trip_stats = data['drt_trips_stats'][iteration]
    filtered_without_router_zeros = it_drt_trip_stats[it_drt_trip_stats.routerUnsharedTime != 0].copy(deep=True)
    filtered_without_router_zeros['abs_difference'] = np.abs(filtered_without_router_zeros['estimatedUnsharedTime'] - filtered_without_router_zeros['routerUnsharedTime'])
    filtered_without_router_zeros['abs_difference_min'] = filtered_without_router_zeros['abs_difference'] / 60 #absolute difference in minutes
    filtered_without_router_zeros['rel_difference'] = filtered_without_router_zeros['abs_difference'] / filtered_without_router_zeros['estimatedUnsharedTime']
    
    cols = ['startTime', 'totalTravelTime', 'routerUnsharedTime', 'estimatedUnsharedTime', 'delayFactor', 'delayFactorEstimatedDRT', 'abs_difference', 'abs_difference_min', 'rel_difference']

    top_abs_diff = filtered_without_router_zeros['abs_difference'].sort_values(ascending=False)[0:n].index.values
    top_rel_diff = filtered_without_router_zeros['rel_difference'].sort_values(ascending=False)[0:n].index.values
    display(filtered_without_router_zeros.loc[top_abs_diff][cols])
    display(filtered_without_router_zeros.loc[top_rel_diff][cols])

def plot_drt_trips_origin(data, iteration=-1):
    trip_stats = data['drt_trips_stats'][iteration].copy(deep=True)
    gdf = gpd.GeoDataFrame(
        trip_stats, geometry=gpd.points_from_xy(trip_stats.startX, trip_stats.startY))
    gdf.crs = 'EPSG:2056'
    gdf.plot()