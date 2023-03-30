import pandas as pd
import numpy as np
from tqdm import tqdm
import os

def read_output(output_directory, last_iter=-1):
    drt_legs = []
    global_avg = []
    global_moving_avg = []
    global_successive_avg = []
    #zonal_avg = []
    #zonal_moving_avg = []
    #zonal_successive_avg = []
    drt_trips_stats = []
    if last_iter == -1:
        last_iter = len(os.listdir(output_directory + '/ITERS')) - 1
    for i in tqdm(range(last_iter + 1)):
        iter_path = os.path.join(output_directory,'ITERS','it.' + str(i)) + \
                    '/' + str(i) + '.'
        global_avg.append(pd.read_csv(iter_path + 'drt_travelTimeData_global.csv', sep=';'))
        global_moving_avg.append(pd.read_csv(iter_path + 'drt_travelTimeData_moving.csv', sep=';'))
        global_successive_avg.append(pd.read_csv(iter_path + 'drt_travelTimeData_successive.csv', sep=';'))
        #zonal_avg.append(pd.read_csv(iter_path + 'DrtWaitTimesZonalAvg.csv', sep=';'))
        #zonal_moving_avg.append(pd.read_csv(iter_path + 'DrtWaitTimesZonalMovingAvg.csv', sep=';'))
        #zonal_successive_avg.append(pd.read_csv(iter_path + 'DrtWaitTimesZonalSuccessiveAvg.csv', sep=';'))
        drt_legs.append(pd.read_csv(iter_path + 'drt_legs_drt.csv', sep=';'))
        drt_trips_stats.append(pd.read_csv(iter_path + 'drt_drtTripsStats.csv', sep=','))


    #link2zones_csv = pd.read_csv(os.path.join(output_directory,'drt_WayneCountyLink2Zones.csv'), sep=';')
    #link2zones_csv = link2zones_csv.set_index('link_id')
    #link2zones = {}
    #for link_id, zone in link2zones_csv.iterrows():
    #    link2zones[link_id] = zone.zone.item()
    
    d = {'drt_legs': drt_legs, 'global_avg': global_avg, 'global_moving_avg': global_moving_avg, 'global_successive_avg': global_successive_avg, 'drt_trips_stats': drt_trips_stats}#, 'zonal_avg': zonal_avg, 'zonal_moving_avg': zonal_moving_avg, 'zonal_successive_avg': zonal_successive_avg, 'link2zones': link2zones}
    return d

def compute_delay_factor_from_estimated(drt_trips_stats):
    for df in tqdm(drt_trips_stats):
        df['delayFactorEstimatedDRT'] = df['totalTravelTime'] / df['estimatedUnsharedTime']