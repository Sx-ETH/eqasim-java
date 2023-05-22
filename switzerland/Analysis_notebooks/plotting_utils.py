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

def get_stats_for_zonal_plot(df, zones, zone_id, metric):
    joined_df = pd.merge(df, zones, on=zone_id, how="right") #merge on right to ensure all zones are showing
    
    ## computing metrics for plot
    mean = joined_df.groupby(zone_id)[metric].mean().fillna(0).reset_index()
    std = joined_df.groupby(zone_id)[metric].std().fillna(0).reset_index()
    count = joined_df.groupby(zone_id)[metric].count().fillna(0).reset_index()
    mean["std"] = std[metric]
    mean["coefficient_of_variation"] = mean["std"] / mean[metric]
    mean["count"] = count[metric]
    mean["standard_error"] = mean["std"] / np.sqrt(mean["count"])
    return mean

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
    scalebar = ScaleBar(1, font_properties={'size': 15}, location="lower left", frameon=False, box_color=None)
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

    ax.text(x=x_min, y=y_min + dy*0.1, s='N', fontsize=15)
    ax.arrow(x=x_min + dx*0.017, y=y_min + dy*0.14, 
           dx=0, dy=0, width=0,
           length_includes_head=False,
           head_width=500, head_length=500, overhang=.2, facecolor='k', edgecolor=None, linewidth=0, 
           head_starts_at_zero=False)
    
    return ax

def plot_zonal_avg(metrics, zones, column, lake_restriction, lakes_path, zurich_districts_path, legend_title, add_map=True, in_subplot=False, vmin=None, vmax=None):
    
    if not in_subplot:
        sns.set_context("poster")
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
              vmin=vmin,
              vmax=vmax,
              legend=True,
              legend_kwds=dict(label=legend_title,
                               orientation="horizontal",
                               shrink=0.7,
                               cax=cax,
                               ),
              alpha= 0.7 if add_map else 1.0, 
              cmap='plasma', 
              edgecolor="grey",
              )
    ax = zurich_overlay(ax,lakes_path, zurich_districts_path, lake_restriction,number_districts=True, district_col="district_id")
    ax = add_scalebar(ax)
    # get color bar
    cax = fig.axes[-1]
    cax.tick_params(labelsize=12)
    # modify font size of color bar
    cax.xaxis.label.set_size(14)
    # add north arrow
    ax = add_north(ax)
    if add_map:
        cx.add_basemap(ax, crs=temp.crs)
    ax.set_axis_off()

    
def plot_districts_wait_time(it_drt_trips_stats, lake_path, zurich_districts_path):
    """
    Plots the wait time by districts
    it_drt_trips_stats: dataframe with drt trips stats from one iteration
    lake_path: path to the lake shapefile in Zurich
    zurich_districts_path: path to the zurich districts shapefile
    """
    it_drt_trips_stats_gpd = utils.convert_drt_legs_to_gpd(it_drt_trips_stats)
    df_districts = get_zurich_districts_gpd(zurich_districts_path)
    imputed_from_legs = impute(it_drt_trips_stats_gpd, df_districts, "trip_id", "district_id",fix_by_distance=False).drop("geometry", axis=1)
    #Since we are not fixing by distance, checking how many points are outside the districts
    print("no. of trips outside the district: ", sum(pd.isna(imputed_from_legs["district_id"])))

    district_metrics_drt_legs = get_metrics_for_zonal_plot(imputed_from_legs, df_districts, "district_id", metrics=["waitTime"])#, "delayFactor", "delayFactor_estimated"])
    plot_zonal_avg(district_metrics_drt_legs, df_districts, 
                              'waitTime', shapely.ops.unary_union([geo for geo in df_districts["geometry"]]),
                             lake_path, zurich_districts_path, 
                             "Average wait time [min]", add_map=True)

def plot_districts_wait_time_standard_error(it_drt_trips_stats, lake_path, zurich_districts_path):
    """
    Plots the wait time mean standard error by districts
    it_drt_trips_stats: dataframe with drt trips stats from one iteration
    lake_path: path to the lake shapefile in Zurich
    zurich_districts_path: path to the zurich districts shapefile
    """
    it_drt_trips_stats_gpd = utils.convert_drt_legs_to_gpd(it_drt_trips_stats)
    df_districts = get_zurich_districts_gpd(zurich_districts_path)
    imputed_from_legs = impute(it_drt_trips_stats_gpd, df_districts, "trip_id", "district_id",fix_by_distance=False).drop("geometry", axis=1)

    district_metrics_drt_legs = get_stats_for_zonal_plot(imputed_from_legs, df_districts, "district_id", "waitTime")
    # Make a bar plot showing the standard error of the wait time
    plt.figure(figsize=(6,6))
    plt.bar(district_metrics_drt_legs.district_id.astype(int), district_metrics_drt_legs["standard_error"])
    plt.xlabel("District ID", fontsize=12)
    plt.xticks(district_metrics_drt_legs.district_id.astype(int), fontsize=12)
    plt.ylabel("Standard error of the wait time [min]", fontsize=12)
    plt.yticks(fontsize=12)
    plt.title("Standard error of the wait time by district", fontsize=14)

    plt.show()

def plot_taz_zones_wait_time(it_drt_trips_stats, lake_path, taz_path, taz_id_field, zurich_districts_path, vmax=None):
    """
    Plots the wait time by districts
    it_drt_trips_stats: dataframe with drt trips stats from one iteration
    lake_path: path to the lake shapefile in Zurich
    taz_path: path to the taz shapefile
    taz_id_field: name of the field in the taz shapefile that contains the taz id
    zurich_districts_path: path to the zurich districts shapefile
    vmax: maximum value for the colorbar
    """
    sns.set_context('notebook')
    it_drt_trips_stats_gpd = utils.convert_drt_legs_to_gpd(it_drt_trips_stats)
    taz = gpd.read_file(taz_path)
    imputed_from_legs = impute(it_drt_trips_stats_gpd, taz, "trip_id", taz_id_field,fix_by_distance=False).drop("geometry", axis=1)
    #Since we are not fixing by distance, checking how many points are outside the districts
    print("no. of trips outside the zones: ", sum(pd.isna(imputed_from_legs[taz_id_field])))
    
    district_metrics_drt_legs = get_metrics_for_zonal_plot(imputed_from_legs, taz, taz_id_field, metrics=["waitTime"])#, "delayFactor", "delayFactor_estimated"])
    
    plt.figure(figsize=(13,7))
    plt.subplot(1,2,1)
    plot_zonal_avg(district_metrics_drt_legs, taz, 
                              'waitTime', shapely.ops.unary_union([geo for geo in taz["geometry"]]),
                             lake_path, zurich_districts_path, 
                             "Average wait time [min]", add_map=True, in_subplot=True, vmax=vmax)
    plt.subplot(1,2,2)
    grouped_by_n_trips = imputed_from_legs.groupby(taz_id_field) \
                                .agg(columnAvg=('waitTime', 'mean'), nTrips=('trip_id','size'))
        
    x = grouped_by_n_trips.nTrips
    y = grouped_by_n_trips.columnAvg / 60
    sns.regplot(x=x,y=y)
    plt.ylim(0, vmax)
    plt.ylabel('Waiting time (min)', fontsize=12)
    plt.xlabel('Number of trips', fontsize=12)
    plt.xticks(fontsize=12)
    plt.yticks(fontsize=12)
    plt.tight_layout()
    plt.show()

def plot_taz_zones_wait_time_cv(it_drt_trips_stats, lake_path, taz_path, taz_id_field, zurich_districts_path, vmax=None,
                               csv_metrics_path=None):
    """
    Plots the wait time by districts
    it_drt_trips_stats: dataframe with drt trips stats from one iteration
    lake_path: path to the lake shapefile in Zurich
    taz_path: path to the taz shapefile
    taz_id_field: name of the field in the taz shapefile that contains the taz id
    zurich_districts_path: path to the zurich districts shapefile
    vmax: maximum value for the colorbar
    """
    sns.set_context('notebook')
    it_drt_trips_stats_gpd = utils.convert_drt_legs_to_gpd(it_drt_trips_stats)
    taz = gpd.read_file(taz_path)
    imputed_from_legs = impute(it_drt_trips_stats_gpd, taz, "trip_id", taz_id_field,fix_by_distance=False).drop("geometry", axis=1)
    #Since we are not fixing by distance, checking how many points are outside the districts
    print("no. of trips outside the zones: ", sum(pd.isna(imputed_from_legs[taz_id_field])))
	
    cv_metrics_drt_legs = get_stats_for_zonal_plot(imputed_from_legs, taz, taz_id_field, "waitTime")
    
    district_metrics_drt_legs = get_metrics_for_zonal_plot(imputed_from_legs, taz, taz_id_field, metrics=["waitTime"])#, "delayFactor", "delayFactor_estimated"])
    
    if csv_metrics_path is not None:
        cv_metrics_drt_legs.to_csv(csv_metrics_path + '/taz_cv_wait_time.csv')

    plt.figure(figsize=(13,7))
    plt.subplot(1,2,1)
    plot_zonal_avg(district_metrics_drt_legs, taz, 
                              'waitTime', shapely.ops.unary_union([geo for geo in taz["geometry"]]),
                             lake_path, zurich_districts_path, 
                             "Average wait time [min]", add_map=True, in_subplot=True, vmax=vmax)
    plt.subplot(1,2,2)
    plot_zonal_avg(cv_metrics_drt_legs, taz, 
                              'coefficient_of_variation', shapely.ops.unary_union([geo for geo in taz["geometry"]]),
                             lake_path, zurich_districts_path, 
                             "Coefficient of variation of wait time", add_map=True, in_subplot=True, vmax=vmax)
    plt.tight_layout()
    plt.show()

def plot_districs_wait_time_cv(it_drt_trips_stats, lake_path, zurich_districts_path, vmax=None,
                               csv_metrics_path=None):
    """
    Plots the wait time by districts
    it_drt_trips_stats: dataframe with drt trips stats from one iteration
    lake_path: path to the lake shapefile in Zurich
    taz_path: path to the taz shapefile
    taz_id_field: name of the field in the taz shapefile that contains the taz id
    zurich_districts_path: path to the zurich districts shapefile
    vmax: maximum value for the colorbar
    """
    sns.set_context('notebook')
    it_drt_trips_stats_gpd = utils.convert_drt_legs_to_gpd(it_drt_trips_stats)
    df_districts = get_zurich_districts_gpd(zurich_districts_path)
    imputed_from_legs = impute(it_drt_trips_stats_gpd, df_districts, "trip_id", "district_id",fix_by_distance=False).drop("geometry", axis=1)

    #Since we are not fixing by distance, checking how many points are outside the districts
	
    cv_metrics_drt_legs = get_stats_for_zonal_plot(imputed_from_legs, df_districts, "district_id", "waitTime")
    if csv_metrics_path is not None:
        cv_metrics_drt_legs.to_csv(csv_metrics_path + '/districts_cv_wait_time.csv')
    return
    district_metrics_drt_legs = get_metrics_for_zonal_plot(imputed_from_legs, taz, taz_id_field, metrics=["waitTime"])#, "delayFactor", "delayFactor_estimated"])
    


    plt.figure(figsize=(13,7))
    plt.subplot(1,2,1)
    plot_zonal_avg(district_metrics_drt_legs, taz, 
                              'waitTime', shapely.ops.unary_union([geo for geo in taz["geometry"]]),
                             lake_path, zurich_districts_path, 
                             "Average wait time [min]", add_map=True, in_subplot=True, vmax=vmax)
    plt.subplot(1,2,2)
    plot_zonal_avg(cv_metrics_drt_legs, taz, 
                              'coefficient_of_variation', shapely.ops.unary_union([geo for geo in taz["geometry"]]),
                             lake_path, zurich_districts_path, 
                             "Coefficient of variation of wait time", add_map=True, in_subplot=True, vmax=vmax)
    plt.tight_layout()
    plt.show()


def plot_multigrid_wait_time(grid_sizes, it_drt_trips_stats, zurich_shp_path, lake_path, zurich_districts_path, map_limit=None, vmax=None):
    """
    Plots the wait time in different grid sizes
    grid_sizes: list of grid sizes
    it_drt_trips_stats: dataframe with drt trips stats from one iteration
    zurich_shp_path: path to the shapefile of zurich
    lake_path: path to the shapefile of the lakes in zurich
    zurich_districts_path: path to the shapefile of the districts in zurich
    map_limit: if not None shapely polygon to limit the map
    """
    it_drt_trips_stats_gpd = utils.convert_drt_legs_to_gpd(it_drt_trips_stats)
    zurich_shp = gpd.read_file(zurich_shp_path)
    n_sizes = len(grid_sizes)
    if (n_sizes % 2) == 1:
        raise ValueError("Number of grid sizes must be even")
    plt.figure(figsize=(13,n_sizes//2*13))
    for idx,gs in enumerate(grid_sizes,start=0):
        plt.subplot(n_sizes,2,idx+1)
        grid = utils.create_grid_from_shapefile(zurich_shp_path, gs)
        lake_limit = zurich_shp.loc[0].geometry
        if map_limit:
            grid = gpd.clip(grid, map_limit)
            lake_limit = map_limit
        imputed = impute(it_drt_trips_stats_gpd, grid, "trip_id", "grid_id",fix_by_distance=False).drop("geometry", axis=1)
        metrics = get_metrics_for_zonal_plot(imputed, grid, "grid_id", metrics=["waitTime"])
        plot_zonal_avg(metrics, grid, 'waitTime', lake_limit, lake_path,
                            zurich_districts_path,"Average wait time [min]",
                              add_map=True, in_subplot=True, vmax=vmax)
        plt.title('grid size = ' + str(gs) + 'm', fontsize=14)
        
        plt.subplot(n_sizes,2,idx+n_sizes+1)
        grouped_by_n_trips = imputed.groupby('grid_id') \
                                .agg(columnAvg=('waitTime', 'mean'), nTrips=('trip_id','size'))
        
        x = grouped_by_n_trips.nTrips
        y = grouped_by_n_trips.columnAvg / 60
        sns.regplot(x=x,y=y)
        plt.ylim(0, vmax)
        plt.ylabel('Waiting time (min)', fontsize=12)
        plt.xlabel('Number of trips', fontsize=12)
        plt.title('grid size = ' + str(gs) + 'm', fontsize=14)
        plt.xticks(fontsize=12)
        plt.yticks(fontsize=12)
        
    plt.tight_layout()
    plt.show()

def plot_multigrid_wait_time_variation_coefficient(grid_sizes, it_drt_trips_stats, zurich_shp_path, lake_path, zurich_districts_path, map_limit=None, vmin=None, vmax=None,
                                                   csv_metrics_path=None):
    """
    Plots the variation coefficient of the wait time in different grid sizes
    grid_sizes: list of grid sizes
    it_drt_trips_stats: dataframe with drt trips stats from one iteration
    zurich_shp_path: path to the shapefile of zurich
    lake_path: path to the shapefile of the lakes in zurich
    zurich_districts_path: path to the shapefile of the districts in zurich
    map_limit: if not None shapely polygon to limit the map
    """
    it_drt_trips_stats_gpd = utils.convert_drt_legs_to_gpd(it_drt_trips_stats)
    zurich_shp = gpd.read_file(zurich_shp_path)
    n_sizes = len(grid_sizes)
    if (n_sizes % 2) == 1:
        raise ValueError("Number of grid sizes must be even")
    plt.figure(figsize=(13,n_sizes//4*13))
    for idx,gs in enumerate(grid_sizes,start=0):
        plt.subplot(n_sizes//2,2,idx+1)
        grid = utils.create_grid_from_shapefile(zurich_shp_path, gs)
        lake_limit = zurich_shp.loc[0].geometry
        if map_limit:
            grid = gpd.clip(grid, map_limit)
            lake_limit = map_limit
        imputed = impute(it_drt_trips_stats_gpd, grid, "trip_id", "grid_id",fix_by_distance=False).drop("geometry", axis=1)
        metrics = get_stats_for_zonal_plot(imputed, grid, "grid_id", "waitTime")
        if csv_metrics_path is not None:
            metrics.to_csv(csv_metrics_path+ '/cv_grid_' + str(gs) + ".csv")
        plot_zonal_avg(metrics, grid, 'coefficient_of_variation', 
                              lake_limit, lake_path, zurich_districts_path,
                                "Coefficient of variation of wait time",
                              add_map=True, in_subplot=True, vmin=vmin, vmax=vmax)
        plt.title('grid size = ' + str(gs) + 'm', fontsize=14)
        
        
    plt.tight_layout()
    plt.show()


def plot_column_by_trip_density_scatter(drt_legs_with_zone_id, zone_id_field, column, limit_n_trips = None):
    """
    Plots the average of a column by the number of trips in a zone
    drt_legs_with_zone_id: a dataframe with the drt legs and a column with the zone id
    zone_id_field: the name of the column with the zone id
    column: the name of the column to plot
    limit_n_trips: if not None, only plots the zones with less than limit_n_trips
    """
    sns.set_context('notebook')
    grouped_by_n_trips = drt_legs_with_zone_id.groupby(zone_id_field) \
                            .agg(columnAvg=(column, 'mean'), nTrips=('trip_id','size'))
    if limit_n_trips:
        grouped_by_n_trips = grouped_by_n_trips[grouped_by_n_trips.nTrips < limit_n_trips]
    x = grouped_by_n_trips.nTrips
    y = grouped_by_n_trips.columnAvg
    sns.regplot(x=x,y=y)

  
def plot_OD_delayFactor_heatmaps(it_drt_trips_stats, zones, zones_id_field, filter_router_zeros=False, vmin=None, vmax=None):
    """
    Plot the delayFactor heatmap binning by origin and destination
    it_drt_trips_stats: dataframe with drt trips stats from one iteration
    zones: dataframe with the zones
    zones_id_field: the name of the column with the zone id
    filter_router_zeros: if True, filters the trips with router delayFactor = 0
    """
    it_drt_trips_stats_gpd = utils.convert_drt_legs_to_gpd(it_drt_trips_stats)
    #df_districts = get_zurich_districts_gpd(zurich_districts_path)
    imputed_from_trips_stats = impute(it_drt_trips_stats_gpd, zones, "trip_id", zones_id_field, fix_by_distance=False).drop("geometry", axis=1)
    imputed_from_trips_stats['geometry'] = imputed_from_trips_stats['destination_geometry']
    imputed_from_trips_stats = imputed_from_trips_stats.rename(columns={zones_id_field: 'origin_zone_id'})
    imputed_from_trips_stats = gpd.GeoDataFrame(imputed_from_trips_stats)
    imputed_from_trips_stats.crs = "epsg:2056"
    imputed_from_trips_stats = impute(imputed_from_trips_stats, zones, "trip_id", zones_id_field, fix_by_distance=False).drop("geometry", axis=1)
    imputed_from_trips_stats = imputed_from_trips_stats.rename(columns={zones_id_field: 'destination_zone_id'})
    
    if filter_router_zeros:
        imputed_from_trips_stats = imputed_from_trips_stats[imputed_from_trips_stats.routerUnsharedTime != 0].copy()
    
    group_by = imputed_from_trips_stats.groupby(['origin_zone_id', 'destination_zone_id'])
    average_df_router = group_by.delayFactor.mean().unstack()
    average_df_estimated = group_by.delayFactorEstimatedDRT.mean().unstack()
    computed_df_router = (group_by.totalTravelTime.sum() / group_by.routerUnsharedTime.sum()).unstack()
    computed_df_estimated = (group_by.totalTravelTime.sum() / group_by.estimatedUnsharedTime.sum()).unstack()
    
    to_plot = [average_df_router, average_df_estimated, computed_df_router, computed_df_estimated]
    titles = ['Avg of DF using router', 'Avg of DF using estimation from DRT', 'Computed DF from sum using router', 'Computed DF from sum estimation from DRT']
    
    fig = plt.figure(figsize=(14,14))
    
    for idx, data in enumerate(to_plot, start=1):
        ax = plt.subplot(2,2,idx)
        im = ax.matshow(data.values, vmin=vmin, vmax=vmax)

        # Show all ticks and label them with the respective list entries
        #display(data)
        plt.xticks(np.arange(len(data.columns)), labels=data.columns.values.astype('int'), fontsize=14)
        plt.yticks(np.arange(len(data.index)), labels=data.index.values.astype('int'), fontsize=14)
        plt.xlabel(data.columns.name, fontsize=14)
        plt.ylabel(data.index.name, fontsize=14)
        ax.xaxis.set_ticks_position('bottom')
        plt.title(titles[idx-1], fontsize=14)
        cbar = ax.figure.colorbar(im, ax=ax)
        cbar.ax.set_ylabel('delayFactor', rotation=-90, va="bottom", fontsize=14)
        cbar.ax.tick_params(labelsize=14)
        if (len(data.columns) > 20):
            plt.xticks([])
            plt.yticks([])
    plt.tight_layout()
    plt.show()

def show_stopwatch(folder):
    return Image(folder + '/stopwatch.png')

def show_modeshare(folder):
    return Image(folder + '/modestats.png')
    
def show_occupancy_profile(folder, iteration=-1):
    if iteration == -1:
        iteration = len(os.listdir(folder + '/ITERS')) - 1
    return Image(folder + '/ITERS/it.' + str(iteration) + '/' + str(iteration) + '.occupancy_time_profiles_Line_drt.png')

def plot_euclidean_distance_ditribution(data, iteration=-1):
    """
    Plots the euclidean distance distribution of the DRT trips, and displays a description of the stats
    data: dictionary with the output dataframes (must contain drt_trips_stats)
    iteration: iteration to plot, -1 for the last one
    """
    it_drt_trips_stats = data['drt_trips_stats'][iteration]
    euclidean_distance = np.sqrt((it_drt_trips_stats.startX -it_drt_trips_stats.endX)**2 + (it_drt_trips_stats.startY -it_drt_trips_stats.endY)**2)
    display(euclidean_distance.describe(percentiles=[.25,.50,.75,.90,.95,.99]).to_frame().transpose())
    plt.hist(euclidean_distance / 1000, range=(0,10), density=True)
    plt.xlabel('Euclidean distance (km)')
    plt.ylabel('Number of DRT trips')
    plt.show()
    #stats.probplot(euclidean_distance, dist=dist, plot=plt, sparams=kwargs)
    #plt.show()

    
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
    if 'euclideanDistance' not in legs.columns:
        legs['euclideanDistance'] = np.sqrt((legs.startX - legs.endX)**2 + (legs.startY - legs.endY)**2)
    legs['distance_bin'] = pd.cut(legs.euclideanDistance, distance_bins).map(lambda x: int((x.left + x.right)/2))
    if column == 'compute_total_delay_factor_estimated':
        grouped = legs.groupby(['distance_bin'])['totalTravelTime'].sum() / legs.groupby(['distance_bin'])['estimatedUnsharedTime'].sum()
    elif column == 'compute_total_delay_factor_router':
        grouped = legs.groupby(['distance_bin'])['totalTravelTime'].sum() / legs.groupby(['distance_bin'])['routerUnsharedTime'].sum()
    else:
        grouped = legs.groupby(['distance_bin'])[column].mean()
    return grouped

def median_by_euclidean_distance_bin(drt_trips_stats, column, min_distance=0, max_distance=5000, bin_distance_m=200):
    legs = drt_trips_stats.copy()
    n_bins = (max_distance - min_distance) // bin_distance_m
    distance_bins = [min_distance + i*bin_distance_m for i in range(n_bins + 1)]
    if 'euclideanDistance' not in legs.columns:
        legs['euclideanDistance'] = np.sqrt((legs.startX - legs.endX)**2 + (legs.startY - legs.endY)**2)
    legs['distance_bin'] = pd.cut(legs.euclideanDistance, distance_bins).map(lambda x: int((x.left + x.right)/2))
    grouped = legs.groupby(['distance_bin'])[column].median()
    return grouped

def plot_delay_factor(data, start_time, end_time, bin_duration_min, min_distance, max_distance, bin_distance_m,
                        iteration=-1, plot_estimated=True, plot_using_sum=True, filter_router_zeros=False,
                        add_boxplots=False, showfliers=False, ylim=None):
    """
    Plots the delay factor for the DRT trips
    data: dictionary with the output dataframes (must contain drt_trips_stats)
    start_time: start time of the time bin (in hours)
    end_time: end time of the time bin (in hours)
    bin_duration_min: duration of the time bin (in minutes)
    min_distance: minimum euclidean distance of the distance bin (in meters)
    max_distance: maximum euclidean distance of the distance bin (in meters)
    bin_distance_m: duration of the distance bin (in meters)
    iteration: iteration to plot, -1 for the last one
    plot_estimated: if True, plots the delay factor using the estimated unshared time done by the DRT module (default: True)
    plot_using_sum: if True, plots the delay factor using the sum of the travel times of the legs and the sum of the predicted times(default: True)
    filter_router_zeros: if True, filters out the trips with routerUnsharedTime = 0 (default: False)
    add_boxplots: if True, adds boxplots for the delay factor (default: False)
    showfliers: if True, shows the outliers in the boxplots (default: False)
    ylim: y limits of the plot (default: None)
    """
    if add_boxplots and (plot_estimated or plot_using_sum):
        raise ValueError('Cannot add boxplots if plot_estimated or plot_using_sum are True')
    it_drt_trip_stats = data['drt_trips_stats'][iteration].copy(deep=True)
    add_title = ''
    if filter_router_zeros:
        it_drt_trip_stats = it_drt_trip_stats[it_drt_trip_stats.routerUnsharedTime != 0]
        add_title = ' \n(filtering the trips with 0 predicted time by the router)'
    delayFactor_avg = avg_by_time_bin(it_drt_trip_stats, 'delayFactor', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
    delayFactor_median = median_by_time_bin(it_drt_trip_stats, 'delayFactor', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
    
    plt.figure(figsize=(15,7.5))
    
    plt.subplot(1,2,1)
    xticks = [z*3600 for z in range(start_time, end_time+1, 2)]
    xticks_labels = [str(z) + 'h' for z in range(start_time, end_time+1, 2)]

    marker = 'o-'
    linewidth = 2

    if add_boxplots:
        time_bins_data, time_bins_middle_points, width = get_data_for_boxplot_time_bin(it_drt_trip_stats, 'delayFactor', start_time, end_time, bin_duration_min)
        plt.boxplot(time_bins_data, positions=time_bins_middle_points, widths=width, showfliers=showfliers,
                    patch_artist=True, boxprops={'color': 'none', 'facecolor': 'red', 'alpha': 0.25},
                    medianprops={'color':'navy'},
                    whiskerprops={'color': 'black', 'linewidth': 0.75})
        marker = '-'
        linewidth = 1.5

    plt.plot(delayFactor_avg.index.values, delayFactor_avg.values, marker, color='red', 
        label='Avg of delay factor using router', linewidth=linewidth, zorder=10)

    if not plot_estimated and not plot_using_sum:
        plt.plot(delayFactor_median.index.values, delayFactor_median.values, marker, color='navy',
                label='Median of delay factor using router', linewidth=linewidth, zorder=10)
    
    
    if plot_using_sum:
        delayFactorComputedRouter_avg = avg_by_time_bin(it_drt_trip_stats, 'compute_total_delay_factor_router', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
        plt.plot(delayFactorComputedRouter_avg.index.values, delayFactorComputedRouter_avg.values, ls='--', marker='o', color='#1f77b4', label='Computed from sum delay factor using router')

    if plot_estimated:
        delayFactorEstimatedDRT_avg = avg_by_time_bin(it_drt_trip_stats, 'delayFactorEstimatedDRT', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
        plt.plot(delayFactorEstimatedDRT_avg.index.values, delayFactorEstimatedDRT_avg.values, ls='-', marker='o', color='#ff7f0e', label='Avg of delay factor using estimated from DRT')
        if plot_using_sum:
            delayFactorComputedEstimatedDRT_avg = avg_by_time_bin(it_drt_trip_stats, 'compute_total_delay_factor_estimated', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
            plt.plot(delayFactorComputedEstimatedDRT_avg.index.values, delayFactorComputedEstimatedDRT_avg.values, ls='--', marker='o', color='#ff7f0e', label='Computed from sum delay factor using estimated from DRT')

    ax = plt.gca()
    ax.axhline(y=it_drt_trip_stats.delayFactor.mean(), color="black", label='Mean of all times', zorder=10, linewidth=1)
    ax.axhline(y=it_drt_trip_stats.delayFactor.median(), color="black",ls = '--', label='Median of all times', zorder=10, linewidth=1)

    if ylim is not None:
        plt.gca().set_ylim(top=ylim)
    
    plt.xlim(start_time*3600,end_time*3600)
    plt.xticks(xticks, xticks_labels, fontsize=12)
    plt.title('Delay Factor by departure time' + add_title)
    plt.ylabel('Delay Factor', fontsize=12)
    plt.xlabel('Time of the day', fontsize=12)
    plt.yticks(fontsize=12)
    
    plt.legend(fontsize=12)

    plt.subplot(1,2,2)
    
    delayFactor_avg = avg_by_euclidean_distance_bin(it_drt_trip_stats, 'delayFactor', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)
    delayFactor_median = median_by_euclidean_distance_bin(it_drt_trip_stats, 'delayFactor', min_distance=min_distance,
                                        max_distance=max_distance, bin_distance_m=bin_distance_m)
    
    if add_boxplots:
        distance_bins_data, distance_bins_middle_points, width = get_data_for_boxplot_distance_bin(it_drt_trip_stats, 'delayFactor', min_distance, max_distance, bin_distance_m)
        plt.boxplot(distance_bins_data, positions=[t/1000 for t in distance_bins_middle_points], widths=width/1000, showfliers=showfliers,
                    patch_artist=True, boxprops={'color': 'none', 'facecolor': 'red', 'alpha': 0.25},
                    medianprops={'color':'navy'},
                    whiskerprops={'color': 'black', 'linewidth': 0.75})

    plt.plot(np.array(delayFactor_avg.index.values)/1000, delayFactor_avg.values, marker, color='red',
                    label='Avg of delay factor using router', linewidth=linewidth, zorder=10)
    if not plot_estimated and not plot_using_sum:
        plt.plot(np.array(delayFactor_median.index.values)/1000, delayFactor_median.values, marker, color='navy',
                label='Median of delay factor using router', linewidth=linewidth, zorder=10)

    if plot_using_sum:
        delayFactorComputedRouter_avg = avg_by_euclidean_distance_bin(it_drt_trip_stats, 'compute_total_delay_factor_router', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)
        plt.plot(np.array(delayFactorComputedRouter_avg.index.values)/1000, delayFactorComputedRouter_avg.values, ls='--', marker='o', color='#1f77b4', label='Computed from sum delay factor using router')
    if plot_estimated:
        delayFactorEstimatedDRT_avg = avg_by_euclidean_distance_bin(it_drt_trip_stats, 'delayFactorEstimatedDRT', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)
        plt.plot(np.array(delayFactorEstimatedDRT_avg.index.values)/1000, delayFactorEstimatedDRT_avg.values, ls='-', marker='o', color='#ff7f0e', label='Avg of delay factor using estimated from DRT')
        if plot_using_sum:
            delayFactorComputedEstimatedDRT_avg = avg_by_euclidean_distance_bin(it_drt_trip_stats, 'compute_total_delay_factor_estimated',min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)
            plt.plot(np.array(delayFactorComputedEstimatedDRT_avg.index.values)/1000, delayFactorComputedEstimatedDRT_avg.values, ls='--', marker='o', color='#ff7f0e', label='Computed from sum delay factor using estimated from DRT')

    ax = plt.gca()
    ax.axhline(y=it_drt_trip_stats.delayFactor.mean(), color="black", label='Mean of all times', zorder=10, linewidth=1)
    ax.axhline(y=it_drt_trip_stats.delayFactor.median(), color="black",ls = '--', label='Median of all times', zorder=10, linewidth=1)

    plt.legend(fontsize=12, loc='upper left')
    plt.title('Delay Factor by euclidean distance' + add_title)
    plt.xlabel('Euclidean distance (km)', fontsize=12)
    plt.ylabel('Delay Factor', fontsize=12)
    plt.xticks(fontsize=12)
    plt.yticks(fontsize=12)
    #plt.tight_layout()
    plt.show()
    
def plot_delay_factor_scatter(data, iteration=-1):
    """
    Plot the waiting time scatter plot
    data: dictionary with the output dataframes (must contain drt_trips_stats)
    iteration: iteration to plot, -1 for the last one
    """
    it_drt_trip_stats = data['drt_trips_stats'][iteration]

    plt.figure(figsize=(15, 7.5))

    plt.subplot(1,2,1)
    x = it_drt_trip_stats['startTime']
    y = it_drt_trip_stats['delayFactor']
    plt.scatter(x,y, marker='x')
    # Calculate Pearson correlation coefficient
    corr_coef = np.corrcoef(x, y)[0, 1]

    xticks = [z*3600 for z in range(0, 25, 2)]
    xticks_labels = [str(z) + 'h' for z in range(0, 25, 2)]
    plt.xticks(xticks, xticks_labels, fontsize=12)
    plt.title('Delay factor vs start time')
    plt.ylabel('Delay factor', fontsize=12)
    plt.xlabel('Time of the day', fontsize=12)
    plt.yticks(fontsize=12)
    plt.text(0.95, 0.95, f'Pearson Corr:\n{corr_coef:.2f}', transform=plt.gca().transAxes, fontsize=12,
                verticalalignment='top', horizontalalignment='right', bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))
    
    plt.subplot(1,2,2)
    x = it_drt_trip_stats['euclideanDistance'] / 1000
    plt.scatter(x,y, marker='x')
    # Calculate Pearson correlation coefficient
    corr_coef = np.corrcoef(x, y)[0, 1]
    plt.xticks(fontsize=12)
    plt.title('Delay factor vs euclidean distance')
    plt.xlabel('Euclidean distance (km)', fontsize=12)
    plt.ylabel('Delay factor', fontsize=12)
    plt.yticks(fontsize=12)
    plt.text(0.95, 0.95, f'Pearson Corr:\n{corr_coef:.2f}', transform=plt.gca().transAxes, fontsize=12,
                verticalalignment='top', horizontalalignment='right', bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))
    plt.show()

def plot_df_multiple_time_bins(data, start_time, end_time, bin_durations_min, iteration=-1, plot_estimated=True, plot_using_sum=True, filter_router_zeros=False, add_boxplots=False, showfliers=False):
    """
    Plot the delay factor for multiple time bins
    data: dictionary with the output dataframes (must contain drt_trips_stats)
    start_time: start time of the time bin (in hours)
    end_time: end time of the time bin (in hours)
    bin_durations_min: durations of the time bin (in minutes)
    iteration: iteration to plot, -1 for the last one
    plot_estimated: if True, plots the delay factor using the estimated unshared time done by the DRT module (default: True)
    plot_using_sum: if True, plots the delay factor using the sum of the travel times of the legs and the sum of the predicted times(default: True)
    filter_router_zeros: if True, filters out the trips with routerUnsharedTime = 0 (default: False)
    add_boxplots: if True, adds boxplots for each time bin (default: False)
    showfliers: if True, shows the outliers in the boxplots (default: False)
    """
    if add_boxplots and (plot_estimated or plot_using_sum):
        raise ValueError('Cannot add boxplots if plot_estimated or plot_using_sum are True')
    it_drt_trip_stats = data['drt_trips_stats'][iteration]
    add_title = ''
    if filter_router_zeros:
        it_drt_trip_stats = it_drt_trip_stats[it_drt_trip_stats.routerUnsharedTime != 0]
        add_title = ' \n(filtering the trips with 0 predicted time by the router)'
    n_rows = (len(bin_durations_min) - 1) // 2 + 1

    plt.figure(figsize=(15, n_rows * 7.5))
    
    xticks = [z*3600 for z in range(start_time, end_time+1, 2)]
    xticks_labels = [str(z) + 'h' for z in range(start_time, end_time+1, 2)]

    marker = 'o-'
    linewidth = 2
    if add_boxplots:
        marker = '-'
        linewidth = 1.5
    
    for idx,time_bin in enumerate(bin_durations_min, start=1):
        delayFactor_avg = avg_by_time_bin(it_drt_trip_stats, 'delayFactor', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=time_bin)
        delayFactor_median = median_by_time_bin(it_drt_trip_stats, 'delayFactor', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=time_bin)
    
        plt.subplot(n_rows,2,idx)
        plt.plot(delayFactor_avg.index.values, delayFactor_avg.values, marker, linewidth=linewidth,
            color='red', label='Avg of delay factor using router', zorder=10)
        if not plot_estimated and not plot_using_sum:
            plt.plot(delayFactor_median.index.values, delayFactor_median.values, marker, color='navy',
                    label='Median of delay factor using router', linewidth=linewidth, zorder=10)
        
        if add_boxplots:
            time_bins_data, time_bins_middle_points, width = get_data_for_boxplot_time_bin(it_drt_trip_stats, 'delayFactor', start_time, end_time, time_bin)
            plt.boxplot(time_bins_data, positions=time_bins_middle_points, widths=width, showfliers=showfliers,
                        patch_artist=True, boxprops={'color': 'none', 'facecolor': 'red', 'alpha': 0.25},
                        medianprops={'color':'navy'},
                        whiskerprops={'color': 'black', 'linewidth': 0.75})

        if plot_using_sum:
            delayFactorComputedRouter_avg = avg_by_time_bin(it_drt_trip_stats, 'compute_total_delay_factor_router', start_time=start_time, 
                                         end_time=end_time, bin_duration_min=time_bin)
            plt.plot(delayFactorComputedRouter_avg.index.values, delayFactorComputedRouter_avg.values, ls='--', marker='o', color='#1f77b4', label='Computed from sum delay factor using router')
        if plot_estimated:
            delayFactorEstimatedDRT_avg = avg_by_time_bin(it_drt_trip_stats, 'delayFactorEstimatedDRT', start_time=start_time, 
                                         end_time=end_time, bin_duration_min=time_bin)
            plt.plot(delayFactorEstimatedDRT_avg.index.values, delayFactorEstimatedDRT_avg.values, ls='-', marker='o', color='#ff7f0e', label='Avg of delay factor using estimated from DRT')
            if plot_using_sum:
                delayFactorComputedEstimatedDRT_avg = avg_by_time_bin(it_drt_trip_stats, 'compute_total_delay_factor_estimated', start_time=start_time, 
                                         end_time=end_time, bin_duration_min=time_bin)
                plt.plot(delayFactorComputedEstimatedDRT_avg.index.values, delayFactorComputedEstimatedDRT_avg.values, ls='--', marker='o', color='#ff7f0e', label='Computed from sum delay factor using estimated from DRT')
        
        ax = plt.gca()
        ax.axhline(y=it_drt_trip_stats.delayFactor.mean(), color="black", label='Mean of all times', zorder=10, linewidth=1)
        ax.axhline(y=it_drt_trip_stats.delayFactor.median(), color="black",ls = '--', label='Median of all times', zorder=10, linewidth=1)


        plt.legend(fontsize=12)
        plt.xlim(start_time*3600,end_time*3600)
        plt.xticks(xticks, xticks_labels, fontsize=12)
        plt.title('Delay Factor by departure time' + add_title + '\nTime bin = ' + str(time_bin) + ' min')
        plt.ylabel('Delay Factor', fontsize=12)
        plt.xlabel('Time of the day', fontsize=12)
        plt.yticks(fontsize=12)
    
    plt.tight_layout()
    plt.show()
        
def plot_df_multiple_distance_bins(data,  min_distance, max_distance, bin_distances_m, iteration=-1, plot_estimated=True, plot_using_sum=True, filter_router_zeros=False, add_boxplots=False, showfliers=False):
    """
    Plot the delay factor for multiple distance bins
    data: dictionary with the output dataframes (must contain drt_trips_stats)
    min_distance: minimum distance of the distance bin (in meters)
    max_distance: maximum distance of the distance bin (in meters)
    bin_distances_m: distances of the distance bin (in meters)
    iteration: iteration to plot, -1 for the last one
    plot_estimated: if True, plots the delay factor using the estimated unshared time done by the DRT module (default: True)
    plot_using_sum: if True, plots the delay factor using the sum of the travel times of the legs and the sum of the predicted times(default: True)
    filter_router_zeros: if True, filters out the trips with routerUnsharedTime = 0 (default: False)
    add_boxplots: if True, adds boxplots for each distance bin (default: False)
    showfliers: if True, shows the outliers in the boxplots (default: False)
    """
    if add_boxplots and (plot_estimated or plot_using_sum):
        raise ValueError('Cannot add boxplots if plot_estimated or plot_using_sum are True')
    it_drt_trip_stats = data['drt_trips_stats'][iteration]
    add_title = ''
    if filter_router_zeros:
        it_drt_trip_stats = it_drt_trip_stats[it_drt_trip_stats.routerUnsharedTime != 0]
        add_title = ' \n(filtering the trips with 0 predicted time by the router)'
    n_rows = (len(bin_distances_m) - 1) // 2 + 1
    plt.figure(figsize=(15, n_rows * 7.5))

    marker = 'o-'
    linewidth = 2
    if add_boxplots:
        marker = '-'
        linewidth = 1.5
    
    for idx,distance_bin in enumerate(bin_distances_m, start=1):
        delayFactor_avg = avg_by_euclidean_distance_bin(it_drt_trip_stats, 'delayFactor', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=distance_bin)
        delayFactor_median = median_by_euclidean_distance_bin(it_drt_trip_stats, 'delayFactor', min_distance=min_distance,
                                        max_distance=max_distance, bin_distance_m=distance_bin)
        plt.subplot(n_rows,2,idx)
     
        plt.plot(np.array(delayFactor_avg.index.values)/1000, delayFactor_avg.values, marker, color='red',
                    label='Avg of delay factor using router', linewidth=linewidth, zorder=10)
        if not plot_estimated and not plot_using_sum:
            plt.plot(np.array(delayFactor_median.index.values)/1000, delayFactor_median.values, marker, color='navy',
                    label='Median of delay factor using router', linewidth=linewidth, zorder=10)
        
        if add_boxplots:
            distance_bins_data, distance_bins_middle_points, width = get_data_for_boxplot_distance_bin(it_drt_trip_stats, 'delayFactor', min_distance, max_distance, distance_bin)
            plt.boxplot(distance_bins_data, positions=[t/1000 for t in distance_bins_middle_points], widths=width/1000, showfliers=showfliers,
                        patch_artist=True, boxprops={'color': 'none', 'facecolor': 'red', 'alpha': 0.25},
                        medianprops={'color':'navy'},
                        whiskerprops={'color': 'black', 'linewidth': 0.75})
        
        if plot_using_sum:
            delayFactorComputedRouter_avg = avg_by_euclidean_distance_bin(it_drt_trip_stats, 'compute_total_delay_factor_router', min_distance=min_distance, 
                                         max_distance=max_distance, bin_distance_m=distance_bin)
            plt.plot(np.array(delayFactorComputedRouter_avg.index.values)/1000, delayFactorComputedRouter_avg.values, ls='--', marker='o', color='#1f77b4', label='Computed from sum delay factor using router')
        if plot_estimated:
            delayFactorEstimatedDRT_avg = avg_by_euclidean_distance_bin(it_drt_trip_stats, 'delayFactorEstimatedDRT', min_distance=min_distance, 
                                         max_distance=max_distance, bin_distance_m=distance_bin)
            plt.plot(np.array(delayFactorEstimatedDRT_avg.index.values)/1000, delayFactorEstimatedDRT_avg.values, ls='-', marker='o', color='#ff7f0e', label='Avg of delay factor using estimated from DRT')
            if plot_using_sum:
                delayFactorComputedEstimatedDRT_avg = avg_by_euclidean_distance_bin(it_drt_trip_stats, 'compute_total_delay_factor_estimated',min_distance=min_distance, 
                                         max_distance=max_distance, bin_distance_m=distance_bin)
                plt.plot(np.array(delayFactorComputedEstimatedDRT_avg.index.values)/1000, delayFactorComputedEstimatedDRT_avg.values, ls='--', marker='o', color='#ff7f0e', label='Computed from sum delay factor using estimated from DRT')
        
        ax = plt.gca()
        ax.axhline(y=it_drt_trip_stats.delayFactor.mean(), color="black", label='Mean of all times', zorder=10, linewidth=1)
        ax.axhline(y=it_drt_trip_stats.delayFactor.median(), color="black",ls = '--', label='Median of all times', zorder=10, linewidth=1)

        plt.legend(fontsize=12, loc='upper left')
        plt.title('Delay Factor by euclidean distance' + add_title + '\nDistance bin = ' + str(distance_bin) + 'm')
        plt.xlabel('Euclidean distance (km)', fontsize=12)
        plt.ylabel('Delay Factor', fontsize=12)
        x_ticks = np.arange(0, max_distance + 1, 1000)
        plt.xticks(x_ticks/1000, x_ticks/1000, fontsize=12)       
        plt.yticks(fontsize=12)
    
    plt.show()

def get_data_for_boxplot_time_bin(it_drt_trip_stats, column, start_time, end_time, bin_duration_min):
    legs = it_drt_trip_stats.copy(deep=True)
    n_time_bins = (end_time - start_time) * 60 // bin_duration_min
    time_bins = [start_time*3600 + i*bin_duration_min*60 for i in range(0,n_time_bins+1)]
    legs['time_bin'] = pd.cut(legs.startTime, time_bins).map(lambda x: int((x.left + x.right)/2))

    time_bins_middle_points = [int((time_bins[i] + time_bins[i+1])/2) for i in range(len(time_bins)-1)]
    time_bins_data = [legs[legs.time_bin == t][column].values for t in time_bins_middle_points]
    width = (time_bins_middle_points[1] - time_bins_middle_points[0]) * 0.5
    return time_bins_data, time_bins_middle_points, width

def get_data_for_boxplot_distance_bin(it_drt_trip_stats, column, min_distance, max_distance, bin_distance_m):
    legs = it_drt_trip_stats.copy(deep=True)
    n_distance_bins = (max_distance - min_distance) // bin_distance_m
    distance_bins = [min_distance + i*bin_distance_m for i in range(n_distance_bins + 1)]
    legs['distance_bin'] = pd.cut(legs.euclideanDistance, distance_bins).map(lambda x: int((x.left + x.right)/2))

    distance_bins_middle_points = [int((distance_bins[i] + distance_bins[i+1])/2) for i in range(len(distance_bins)-1)]
    distance_bins_data = [legs[legs.distance_bin == t][column].values for t in distance_bins_middle_points]
    width = (distance_bins_middle_points[1] - distance_bins_middle_points[0]) * 0.5
    return distance_bins_data, distance_bins_middle_points, width
    
def plot_waiting_time(data, start_time, end_time, bin_duration_min, min_distance, max_distance, bin_distance_m,
                        iteration=-1, filter_router_zeros=False, add_boxplots=False, showfliers=False, ylim=None):
    """
    Plot the waiting time for a given time bin and a given distance bin
    data: dictionary with the output dataframes (must contain drt_trips_stats)
    start_time: start time of the time bin (in hours)
    end_time: end time of the time bin (in hours)
    bin_duration_min: duration of the time bin (in minutes)
    min_distance: minimum distance of the distance bin (in meters)
    max_distance: maximum distance of the distance bin (in meters)
    bin_distance_m: distances of the distance bin (in meters)
    iteration: iteration to plot, -1 for the last one
    filter_router_zeros: if True, filters out the trips with routerUnsharedTime = 0 (default: False)
    add_boxplots: if True, adds boxplots for the waiting time (default: False)
    showfliers: if True, shows the outliers in the boxplots (default: False)
    ylim: superior y limit of the plot (default: None)
    """
    it_drt_trip_stats = data['drt_trips_stats'][iteration]
    add_title = ''
    if filter_router_zeros:
        it_drt_trip_stats = it_drt_trip_stats[it_drt_trip_stats.routerUnsharedTime != 0]
        add_title = ' \n(filtering the trips with 0 predicted time by the router)'
    
    waitTime_avg = avg_by_time_bin(it_drt_trip_stats, 'waitTime', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
    
    waitTime_median = median_by_time_bin(it_drt_trip_stats, 'waitTime', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=bin_duration_min)
    
    plt.figure(figsize=(15,7.5))
    
    plt.subplot(1,2,1)
    xticks = [z*3600 for z in range(start_time, end_time+1, 2)]
    xticks_labels = [str(z) + 'h' for z in range(start_time, end_time+1, 2)]
    marker = 'o-'
    linewidth = 2
    if add_boxplots:
        time_bins_data, time_bins_middle_points, width = get_data_for_boxplot_time_bin(it_drt_trip_stats, 'waitTime', start_time, end_time, bin_duration_min)
        plt.boxplot([t/60 for t in time_bins_data], positions=time_bins_middle_points, widths=width, showfliers=showfliers,
                    patch_artist=True, boxprops={'color': 'none', 'facecolor': 'red', 'alpha': 0.25},
                    medianprops={'color':'navy'},
                    whiskerprops={'color': 'black', 'linewidth': 0.75})
        linewidth = 1.5
        marker = '-'
    
    plt.plot(waitTime_avg.index.values, waitTime_avg.values/60, marker, 
                color='red',label='Avg of wait time', linewidth=linewidth, zorder=10)
    plt.plot(waitTime_median.index.values, waitTime_median.values/60, marker,
                color='navy',label='Median of wait time', linewidth=linewidth, zorder=10)

    ax = plt.gca()
    ax.axhline(y=it_drt_trip_stats.waitTime.mean()/60, color="black", label='Mean of all times', zorder=10, linewidth=1)
    ax.axhline(y=it_drt_trip_stats.waitTime.median()/60, color="black",ls = '--', label='Median of all times', zorder=10, linewidth=1)

    plt.gca().set_ylim(bottom=0)
    if ylim is not None:
        plt.ylim(0,ylim)
    plt.xlim(start_time*3600,end_time*3600)
    plt.xticks(xticks, xticks_labels, fontsize=12)
    plt.title('Wait time by departure time' + add_title)
    plt.ylabel('Wait time (min)', fontsize=12)
    plt.xlabel('Time of the day', fontsize=12)
    plt.yticks(fontsize=12)
    
    plt.legend(loc='upper left', fontsize=12)
    
    
    plt.subplot(1,2,2)
    waitTime_avg = avg_by_euclidean_distance_bin(it_drt_trip_stats, 'waitTime', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)
    waitTime_median = median_by_euclidean_distance_bin(it_drt_trip_stats, 'waitTime', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=bin_distance_m)

    if add_boxplots:
        distance_bins_data, distance_bins_middle_points, width = get_data_for_boxplot_distance_bin(it_drt_trip_stats, 'waitTime', min_distance, max_distance, bin_distance_m)
        plt.boxplot([t/60 for t in distance_bins_data], positions=[t/1000 for t in distance_bins_middle_points], widths=width/1000, showfliers=showfliers,
                    patch_artist=True, boxprops={'color': 'none', 'facecolor': 'red', 'alpha': 0.25},
                    medianprops={'color':'navy'},
                    whiskerprops={'color': 'black', 'linewidth': 0.75})
            
    plt.plot(np.array(waitTime_avg.index.values) / 1000, waitTime_avg.values/60, marker, 
            color='red', linewidth=linewidth, label='Avg of wait time', zorder=10)
    plt.plot(np.array(waitTime_median.index.values) / 1000, waitTime_median.values/60, marker,
            color='navy', linewidth=linewidth, label='Median of wait time', zorder=10)
    plt.gca().set_ylim(bottom=0)
    if ylim is not None:
        plt.ylim(0,ylim)
    ax = plt.gca()
    ax.axhline(y=it_drt_trip_stats.waitTime.mean()/60, color="black", label='Mean of all distances', zorder=10, linewidth=1)
    ax.axhline(y=it_drt_trip_stats.waitTime.median()/60, color="black",ls = '--', label='Median of all distances', zorder=10, linewidth=1)

    plt.title('Wait time by euclidean distance' + add_title)
    plt.xlabel('Euclidean distance (km)', fontsize=12)
    plt.ylabel('Wait time (min)', fontsize=12)
    plt.legend(loc='upper right', fontsize=12)
    plt.yticks(fontsize=12)
    plt.xticks(fontsize=12)
    
    plt.show()


def plot_waiting_time_scatter(data, iteration=-1):
    """
    Plot the waiting time scatter plot
    data: dictionary with the output dataframes (must contain drt_trips_stats)
    iteration: iteration to plot, -1 for the last one
    """
    it_drt_trip_stats = data['drt_trips_stats'][iteration]

    plt.figure(figsize=(15, 7.5))

    plt.subplot(1,2,1)
    x = it_drt_trip_stats['startTime']
    y = it_drt_trip_stats['waitTime']/ 60
    plt.scatter(x,y, marker='x')
    # Calculate Pearson correlation coefficient
    corr_coef = np.corrcoef(x, y)[0, 1]

    xticks = [z*3600 for z in range(0, 25, 2)]
    xticks_labels = [str(z) + 'h' for z in range(0, 25, 2)]
    plt.xticks(xticks, xticks_labels, fontsize=12)
    plt.title('Waiting time vs start time')
    plt.ylabel('Waiting time (min)', fontsize=12)
    plt.xlabel('Time of the day', fontsize=12)
    plt.yticks(fontsize=12)
    plt.text(0.95, 0.95, f'Pearson Corr:\n{corr_coef:.2f}', transform=plt.gca().transAxes, fontsize=12,
                verticalalignment='top', horizontalalignment='right', bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))
    
    plt.subplot(1,2,2)
    x = it_drt_trip_stats['euclideanDistance'] / 1000
    plt.scatter(x,y, marker='x')
    # Calculate Pearson correlation coefficient
    corr_coef = np.corrcoef(x, y)[0, 1]
    plt.xticks(fontsize=12)
    plt.title('Waiting time vs euclidean distance')
    plt.xlabel('Euclidean distance (km)', fontsize=12)
    plt.yticks(fontsize=12)
    plt.ylabel('Waiting time (min)', fontsize=12)
    plt.text(0.95, 0.95, f'Pearson Corr:\n{corr_coef:.2f}', transform=plt.gca().transAxes, fontsize=12,
                verticalalignment='top', horizontalalignment='right', bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))
    plt.show()



def plot_waiting_time_multiple_time_bins(data, start_time, end_time, bin_durations_min, iteration=-1, filter_router_zeros=False, add_boxplots=False, showfliers=False, ylim=None):
    """
    Plot the waiting time for multiple time bins
    data: dictionary with the output dataframes (must contain drt_trips_stats)
    start_time: start time of the time bin (in hours)
    end_time: end time of the time bin (in hours)
    bin_durations_min: list of durations of the time bins (in minutes)
    iteration: iteration to plot, -1 for the last one
    filter_router_zeros: if True, filters out the trips with routerUnsharedTime = 0 (default: False)
    add_boxplots: if True, adds boxplots for each time bin (default: False)
    showfliers: if True, shows the outliers in the boxplots (default: False)
    ylim: y axis limit (default: None)
    """
    it_drt_trip_stats = data['drt_trips_stats'][iteration]
    add_title = ''
    if filter_router_zeros:
        it_drt_trip_stats = it_drt_trip_stats[it_drt_trip_stats.routerUnsharedTime != 0]
        add_title = ' \n(filtering the trips with 0 predicted time by the router)'
    n_rows = (len(bin_durations_min) - 1) // 2 + 1
    plt.figure(figsize=(15, n_rows * 7.5))
    
    xticks = [z*3600 for z in range(start_time, end_time+1, 2)]
    xticks_labels = [str(z) + 'h' for z in range(start_time, end_time+1, 2)]

    marker = 'o-'
    linewidth = 2
    if add_boxplots:
        marker = '-'
        linewidth = 1.5
    
    for idx,time_bin in enumerate(bin_durations_min, start=1):
        waitTime_avg = avg_by_time_bin(it_drt_trip_stats, 'waitTime', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=time_bin)
        waitTime_median = median_by_time_bin(it_drt_trip_stats, 'waitTime', start_time=start_time, 
                                     end_time=end_time, bin_duration_min=time_bin)
        
        plt.subplot(n_rows,2,idx)

        if add_boxplots:
            time_bins_data, time_bins_middle_points, width = get_data_for_boxplot_time_bin(it_drt_trip_stats, 'waitTime', start_time, end_time, time_bin)
            plt.boxplot([t/60 for t in time_bins_data], positions=time_bins_middle_points, widths=width, showfliers=showfliers,
                            patch_artist=True, boxprops={'color': 'none', 'facecolor': 'red', 'alpha': 0.25},
                            medianprops={'color':'navy'},
                            whiskerprops={'color': 'black', 'linewidth': 0.75})

        plt.plot(waitTime_avg.index.values, waitTime_avg.values/60, marker, 
                    color='red',label='Avg of wait time', linewidth=linewidth, zorder=10)
        plt.plot(waitTime_median.index.values, waitTime_median.values/60, marker,
                    color='navy',label='Median of wait time', linewidth=linewidth, zorder=10)
        plt.xlim(start_time*3600,end_time*3600)
        plt.xticks(xticks, xticks_labels, fontsize=12)
        plt.yticks(fontsize=12)
        plt.title('Wait time by departure time' + add_title + '\nTime bin = ' + str(time_bin) + ' min')
        plt.ylabel('Wait time (min)', fontsize=12)
        plt.xlabel('Time of the day', fontsize=12)
        plt.gca().set_ylim(bottom=0)
        if ylim is not None:
            plt.ylim(0,ylim)
        ax = plt.gca()
        ax.axhline(y=it_drt_trip_stats.waitTime.mean()/60, color="black", label='Mean of all times', zorder=10, linewidth=1)
        ax.axhline(y=it_drt_trip_stats.waitTime.median()/60, color="black",ls = '--', label='Median of all times', zorder=10, linewidth=1)
        plt.legend(loc='upper left', fontsize=12)
    
    plt.show()

def plot_waiting_time_multiple_distance_bins(data,  min_distance, max_distance, bin_distances_m, iteration=-1, filter_router_zeros=False, add_boxplots=False, showfliers=False, ylim=None):
    """
    Plot the waiting time for multiple distance bins
    data: dictionary with the output dataframes (must contain drt_trips_stats)
    min_distance: minimum distance of the distance bin (in meters)
    max_distance: maximum distance of the distance bin (in meters)
    bin_distances_m: list of distances of the distance bins (in meters)
    iteration: iteration to plot, -1 for the last one
    filter_router_zeros: if True, filters out the trips with routerUnsharedTime = 0 (default: False)
    add_boxplots: if True, adds boxplots for each distance bin (default: False)
    showfliers: if True, shows the outliers in the boxplots (default: False)
    ylim: y axis limit (default: None)
    """
    it_drt_trip_stats = data['drt_trips_stats'][iteration]
    add_title = ''
    if filter_router_zeros:
        it_drt_trip_stats = it_drt_trip_stats[it_drt_trip_stats.routerUnsharedTime != 0]
        add_title = ' \n(filtering the trips with 0 predicted time by the router)'
    n_rows = (len(bin_distances_m) - 1) // 2 + 1
    plt.figure(figsize=(15, n_rows * 7.5))

    marker = 'o-'
    linewidth = 2
    if add_boxplots:
        marker = '-'
        linewidth = 1.5
    
    for idx,distance_bin in enumerate(bin_distances_m, start=1):
        plt.subplot(n_rows,2,idx)
        waitTime_avg = avg_by_euclidean_distance_bin(it_drt_trip_stats, 'waitTime', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=distance_bin)
        waitTime_median = median_by_euclidean_distance_bin(it_drt_trip_stats, 'waitTime', min_distance=min_distance, 
                                     max_distance=max_distance, bin_distance_m=distance_bin)

        if add_boxplots:
            distance_bins_data, distance_bins_middle_points, width = get_data_for_boxplot_distance_bin(it_drt_trip_stats, 'waitTime', min_distance, max_distance, distance_bin)
            plt.boxplot([t/60 for t in distance_bins_data], positions=[t/1000 for t in distance_bins_middle_points], widths=width/1000, showfliers=showfliers,
                        patch_artist=True, boxprops={'color': 'none', 'facecolor': 'red', 'alpha': 0.25},
                        medianprops={'color':'navy'},
                        whiskerprops={'color': 'black', 'linewidth': 0.75})

        plt.plot(np.array(waitTime_avg.index.values) / 1000, waitTime_avg.values/60, marker, 
            color='red', linewidth=linewidth, label='Avg of wait time', zorder=10)
        plt.plot(np.array(waitTime_median.index.values) / 1000, waitTime_median.values/60, marker,
                color='navy', linewidth=linewidth, label='Median of wait time', zorder=10)
        ax = plt.gca()
        ax.axhline(y=it_drt_trip_stats.waitTime.mean()/60, color="black", label='Mean of all distances', zorder=10, linewidth=1)
        ax.axhline(y=it_drt_trip_stats.waitTime.median()/60, color="black",ls = '--', label='Median of all distances', zorder=10, linewidth=1)

        plt.gca().set_ylim(bottom=0)
        if ylim is not None:
            plt.ylim(0,ylim)
        plt.title('Wait time by euclidean distance' + add_title + '\nDistance bin = ' + str(distance_bin) + 'm')
        plt.xlabel('Euclidean distance (km)', fontsize=12)
        plt.ylabel('Wait time (min)', fontsize=12)
        plt.legend(loc='upper right', fontsize=12)
        plt.yticks(fontsize=12)
        x_ticks = np.arange(0, max_distance + 1, 1000)
        plt.xticks(x_ticks/1000, x_ticks/1000, fontsize=12)
    
    plt.show()

def get_stats_table(it_drt_trip_stats):
    index_list = [("Number of rides", ""),
                    ("Wait time (min)", "Mean"),
                    ("Wait time (min)", "Median"),
                    ("Wait time (min)", "Std"),
                    ("Wait time (min)", "75-percentile"),
                    ("Wait time (min)", "99-percentile"),
                    ("Wait time (min)", "Max"),
                    ("Travel time (min)", "Mean"),
                    ("Travel time (min)", "Median"),
                    ("Travel time (min)", "Std"),
                    ("Travel time (min)", "75-percentile"),
                    ("Travel time (min)", "99-percentile"),
                    ("Travel time (min)", "Max"),
                    #("Mean distance (km)", ""),
                    ("Mean direct distance (km)", ""),
                    ("Average detour factor (time wise)", "")
                  ]

    index = pd.MultiIndex.from_tuples(index_list)
    stats = pd.DataFrame(index=index)
    stats.loc[("Number of rides", ""), "Value"] = len(it_drt_trip_stats)
    stats.loc[("Wait time (min)", "Mean"), "Value"] = it_drt_trip_stats.waitTime.mean() / 60
    stats.loc[("Wait time (min)", "Median"), "Value"] = it_drt_trip_stats.waitTime.median() / 60
    stats.loc[("Wait time (min)", "Std"), "Value"] = it_drt_trip_stats.waitTime.std() / 60
    stats.loc[("Wait time (min)", "75-percentile"), "Value"] = it_drt_trip_stats.waitTime.quantile(0.75) / 60
    stats.loc[("Wait time (min)", "99-percentile"), "Value"] = it_drt_trip_stats.waitTime.quantile(0.99) / 60
    stats.loc[("Wait time (min)", "Max"), "Value"] = it_drt_trip_stats.waitTime.max() / 60
    stats.loc[("Travel time (min)", "Mean"), "Value"] = it_drt_trip_stats.totalTravelTime.mean() / 60
    stats.loc[("Travel time (min)", "Median"), "Value"] = it_drt_trip_stats.totalTravelTime.median() / 60
    stats.loc[("Travel time (min)", "Std"), "Value"] = it_drt_trip_stats.totalTravelTime.std() / 60
    stats.loc[("Travel time (min)", "75-percentile"), "Value"] = it_drt_trip_stats.totalTravelTime.quantile(0.75) / 60
    stats.loc[("Travel time (min)", "99-percentile"), "Value"] = it_drt_trip_stats.totalTravelTime.quantile(0.99) / 60
    stats.loc[("Travel time (min)", "Max"), "Value"] = it_drt_trip_stats.totalTravelTime.max() / 60
    #stats.loc[("Mean distance (km)", ""), "Value"] = it_drt_trip_stats.distance.mean() / 1000
    stats.loc[("Mean direct distance (km)", ""), "Value"] = it_drt_trip_stats.euclideanDistance.mean() / 1000
    stats.loc[("Average detour factor (time wise)", ""), "Value"] = it_drt_trip_stats.delayFactor.mean()
    return stats

def get_multiple_stats_table(dictionary):
    tables = []
    for key, value in dictionary.items():
        t = get_stats_table(value)
        t.rename(columns={'Value': key}, inplace=True)
        tables.append(t)
    return pd.concat(tables, axis=1)



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