import os
os.environ['USE_PYGEOS'] = '0'
import geopandas as gpd
import pandas as pd
from shapely.geometry import LineString, Point
import matplotlib.pyplot as plt
from matplotlib_scalebar.scalebar import ScaleBar
import contextily as cx



from plotting_utils import impute, get_metrics_for_zonal_plot

def convert_drt_legs_to_gpd(it_drt_legs):
    df = it_drt_legs.copy()
    if 'startX' in df.columns:
        df['origin_geometry'] = [Point(xy) for xy in zip(df.startX, df.startY)]
        df['destination_geometry'] = [Point(xy) for xy in zip(df.endX, df.endY)]
    else:
        df['origin_geometry'] = [Point(xy) for xy in zip(df.fromX, df.fromY)]
        df['destination_geometry'] = [Point(xy) for xy in zip(df.toX, df.toY)]
    df['geometry'] = df['origin_geometry']
    df = gpd.GeoDataFrame(df)
    df.crs = "epsg:2154"
    df["trip_id"] = [x for x in range(1, len(df)+1)]

    return df

def plot_zonal_avg_helper(metrics, zones, metric, legend_title, in_subplot=False, add_map=True, vmin=None, vmax=None):
    if not in_subplot:
        plt.figure(figsize=(10, 10))
    fig = plt.gcf()
    ax = plt.gca()

    # allow to adjust colormap
    from mpl_toolkits.axes_grid1 import make_axes_locatable
    divider = make_axes_locatable(ax)
    cax = divider.new_vertical(size='5%', pad=0.6, pack_start = True)
    fig.add_axes(cax)

    temp = gpd.GeoDataFrame(pd.merge(metrics, zones))
    
    temp.plot(column=metric,
              ax=ax,
              legend=True,
              cmap='plasma',
              legend_kwds=dict(label=legend_title,
                               orientation="horizontal",
                               cax=cax,
                               shrink=0.7
                               ),
              edgecolor="grey",
              alpha=0.7 if add_map else 1,
              vmin=vmin,
              vmax=vmax
    )
    # add scale bar
    scalebar = ScaleBar(1, font_properties={'size': 15}, location="lower left", frameon=False, box_color=None)
    ax.add_artist(scalebar)
    
    # get color bar
    cax = fig.axes[-1]
    cax.tick_params(labelsize=12)
    # modify font size of color bar
    cax.xaxis.label.set_size(14)
    if add_map:
        cx.add_basemap(ax, crs=temp.crs)

    ax.set_axis_off()

def plot_average_waitTime(it_drt_trips_stats, zones, zones_id, add_empty=False):
    it_drt_trips_stats_gpd = convert_drt_legs_to_gpd(it_drt_trips_stats)
    imputed = impute(it_drt_trips_stats_gpd, zones, "trip_id", zones_id,fix_by_distance=False).drop("geometry", axis=1)
    metrics = get_metrics_for_zonal_plot(imputed, zones, zones_id, ["waitTime"], add_empty=add_empty)
    
    plot_zonal_avg_helper(metrics, zones, "waitTime", "Average waiting time [min]")

    return metrics


def plot_average_waitTime_per_timeBins(it_drt_trips_stats, zones, zones_id, add_empty=False, vmin=None, vmax=None):
    hours_pairs = [(0,24), (7, 9), (9, 11), (11, 15), (15, 19), (19, 7)]

    it_drt_trips_stats_gpd = convert_drt_legs_to_gpd(it_drt_trips_stats)
    imputed = impute(it_drt_trips_stats_gpd, zones, "trip_id", zones_id,fix_by_distance=False).drop("geometry", axis=1)

    plt.figure(figsize=(15, 25))

    for i, (start, end) in enumerate(hours_pairs):
        if start < end:
            selected = imputed[(imputed["startTime"] >= start*3600) & (imputed["startTime"] < end*3600)]
        else:
            selected = imputed[(imputed["startTime"] >= start*3600) | (imputed["startTime"] < end*3600)]
        metrics = get_metrics_for_zonal_plot(selected, zones, zones_id, ["waitTime"], add_empty=add_empty)
        plt.subplot(3, 2, i+1)
        plot_zonal_avg_helper(metrics, zones, "waitTime", "Average waiting time [min]", in_subplot=True, add_map=True, vmin=vmin, vmax=vmax)
        plt.title(f"Time: {start}-{end}h")
    plt.show()

def plot_average_waitTime_per_timeBins_custom(it_drt_trips_stats, zones, zones_id, add_empty=False, vmin=None, vmax=None):
    hours_pairs = [(7, 10), (12,15)]

    it_drt_trips_stats_gpd = convert_drt_legs_to_gpd(it_drt_trips_stats)
    imputed = impute(it_drt_trips_stats_gpd, zones, "trip_id", zones_id,fix_by_distance=False).drop("geometry", axis=1)

    plt.figure(figsize=(15, 8))

    for i, (start, end) in enumerate(hours_pairs):
        if start < end:
            selected = imputed[(imputed["startTime"] >= start*3600) & (imputed["startTime"] < end*3600)]
        else:
            selected = imputed[(imputed["startTime"] >= start*3600) | (imputed["startTime"] < end*3600)]
        metrics = get_metrics_for_zonal_plot(selected, zones, zones_id, ["waitTime"], add_empty=add_empty)
        plt.subplot(1, 2, i+1)
        plot_zonal_avg_helper(metrics, zones, "waitTime", "Average waiting time [min]", in_subplot=True, add_map=True, vmin=vmin, vmax=vmax)
        plt.title(f"Time: {start}-{end}h")
    plt.show()

def plot_average_waitTime_per_fleeSize(data, add_empty=False, vmin=None, vmax=None):
    fleetSizes = list(data.keys())
    fleetSizes.sort()
    assert len(fleetSizes) == 10, "There should be 10 fleet sizes"

    plt.figure(figsize=(15, 40))

    for i, fleetSize in enumerate(fleetSizes):
        it_drt_trips_stats = data[fleetSize]['drt_trips_stats'][-1].copy(deep=True)
        it_drt_trips_stats_gpd = convert_drt_legs_to_gpd(it_drt_trips_stats)
        zones = data[fleetSize]['fixedZones'].copy(deep=True)
        zones.crs = "epsg:2154"
        zones_id = "zoneId"

        imputed = impute(it_drt_trips_stats_gpd, zones, "trip_id", zones_id,fix_by_distance=False).drop("geometry", axis=1)
        metrics = get_metrics_for_zonal_plot(imputed, zones, zones_id, ["waitTime"], add_empty=add_empty)
        plt.subplot(5, 2, i+1)
        plot_zonal_avg_helper(metrics, zones, "waitTime", "Average waiting time [min]", in_subplot=True, add_map=True, vmin=vmin, vmax=vmax)
        plt.title(f"Fleet size: {fleetSize}")
    plt.show()

