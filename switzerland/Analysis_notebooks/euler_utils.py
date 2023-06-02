import pandas as pd
import numpy as np
from tqdm import tqdm
import os
import geopandas as gpd

def read_output(output_directory, last_iter=-1):
    drt_legs = []
    global_stats = []
    drt_trips_stats = []
    drt_predictions = []
    
    iter_0_path = os.path.join(output_directory,'ITERS','it.0')
    binned = '0.drt_zonalAndTimeBinWaitingTime.csv' in os.listdir(iter_0_path)
    if binned:
        binned_wait_time = []
        binned_delay_factor = []
    
    if last_iter == -1:
        last_iter = len(os.listdir(output_directory + '/ITERS')) - 1
    for i in tqdm(range(last_iter+1)):
        iter_path = os.path.join(output_directory,'ITERS','it.' + str(i)) + \
                    '/' + str(i) + '.'
        global_stats.append(pd.read_csv(iter_path + 'drt_globalStats.csv', sep=';'))
        if binned:
            binned_wait_time.append(pd.read_csv(iter_path + 'drt_zonalAndTimeBinWaitingTime.csv', sep=';'))
            binned_delay_factor.append(pd.read_csv(iter_path + 'drt_distanceAndTimeBinDelayFactor.csv', sep=';'))
        
        drt_legs.append(pd.read_csv(iter_path + 'drt_legs_drt.csv', sep=';'))
        drt_trips_stats.append(pd.read_csv(iter_path + 'drt_drtTripsStats.csv', sep=';'))
        drt_predictions.append(pd.read_csv(iter_path + 'drt_drtTripsPredictions.csv', sep=';'))

        
    d = {'drt_legs': drt_legs,
         'global_stats': global_stats,
         'drt_trips_stats': drt_trips_stats,
         'drt_predictions': drt_predictions
        }
    
    if binned:
        d['binned_wait_time'] = binned_wait_time
        d['binned_delay_factor'] = binned_delay_factor
    
    if 'drt_link2FixedZones.csv' in os.listdir(output_directory):
        link2zones_csv = pd.read_csv(os.path.join(output_directory,'drt_link2FixedZones.csv'), sep=';')
        link2zones_csv = link2zones_csv.set_index('link_id')
        link2zones = {}
        for link_id, zone in link2zones_csv.iterrows():
            link2zones[link_id] = zone.zone
        fixed_zones = gpd.read_file(os.path.join(output_directory,'drt_FixedZones.shp'))
        d['link2zones'] = link2zones
        d['fixedZones'] = fixed_zones
    #Read stopwatch.txt
    stopwatch = pd.read_csv(os.path.join(output_directory,'stopwatch.txt'), sep='\t')
    d['stopwatch'] = stopwatch
    
    return d

def compute_delay_factor_from_estimated(drt_trips_stats):
    for df in tqdm(drt_trips_stats):
        df['delayFactorEstimatedDRT'] = df['totalTravelTime'] / df['estimatedUnsharedTime']