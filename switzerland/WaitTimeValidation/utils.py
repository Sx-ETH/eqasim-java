import geopandas as gpd
import pandas as pd
import numpy as np
from shapely.geometry import Polygon
import os


def create_grid_from_shapefile(shapefile, grid_size):
    """Create a grid from a shapefile and a grid size.

    Parameters
    ----------
    shapefile : str
        Path to the shapefile.
    grid_size : float
        Size of the grid.

    Returns
    -------
    grid : geopandas.GeoDataFrame
        Grid as a GeoDataFrame.
    """
    # Read the shapefile
    shape = gpd.read_file(shapefile)

    # Get the bounding box
    xmin, ymin, xmax, ymax = shape.total_bounds

    rows = int(np.ceil((ymax-ymin) /  grid_size))
    cols = int(np.ceil((xmax-xmin) / grid_size))

    XleftOrigin = xmin
    XrightOrigin = xmin + grid_size
    YtopOrigin = ymax
    YbottomOrigin = ymax - grid_size
    polygons = []
    ids = []
    for i in range(cols):
        Ytop = YtopOrigin
        Ybottom = YbottomOrigin
        for j in range(rows):
            polygons.append(Polygon([(XleftOrigin, Ytop), (XrightOrigin, Ytop), (XrightOrigin, Ybottom), (XleftOrigin, Ybottom)])) 
            ids.append(str(i) + '_' + str(j))
            Ytop = Ytop - grid_size
            Ybottom = Ybottom - grid_size
        XleftOrigin = XleftOrigin + grid_size
        XrightOrigin = XrightOrigin + grid_size

    grid = gpd.GeoDataFrame({'geometry':polygons, 'grid_id':ids})
    grid.crs = shape.crs

    # Clip the grid to the shapefile
    grid = gpd.clip(grid, shape)

    return grid


def get_drt_legs(output_directory):
    drt_legs = []
    iters_dir = os.path.join(output_directory, 'testDrtZones', 'ITERS')
    iters_folders = [os.path.join(iters_dir, f) for f in os.listdir(iters_dir) if os.path.isdir(os.path.join(iters_dir, f))]
    iters_folders.sort()
    for it, it_folder in enumerate(iters_folders):
        iter_path = it_folder + '/' + str(it) + '.'
        drt_legs.append(pd.read_csv(iter_path + 'drt_legs_drt.csv', sep=';'))
    
    return drt_legs
