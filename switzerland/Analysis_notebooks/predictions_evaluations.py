import pandas as pd
import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt
import matplotlib.ticker as mticker


def combine_predictions_and_stats(predictions, stats):
    predictions = predictions.copy(deep=True)
    stats = stats.copy(deep=True)
    
    predictions = predictions.add_suffix('_pred')
    predictions = predictions.rename(index=str, columns={'personId_pred':'personId', 'tripIndex_pred':'tripIndex'})

    stats = stats.add_suffix('_stats')
    stats = stats.rename(index=str, columns={'personId_stats':'personId', 'tripIndex_stats':'tripIndex'})
    
    return pd.merge(predictions, stats,
         on=['personId','tripIndex'], validate='one_to_one')

def plot_actual_vs_fitted_waitTime(data, iteration=-1):
    iteration = len(data['drt_predictions'])-1 if iteration == -1 else iteration
    it_drt_predictions = data['drt_predictions'][iteration].copy(deep=True)
    it_drt_trip_stats = data['drt_trips_stats'][iteration].copy(deep=True)
    merged = combine_predictions_and_stats(it_drt_predictions,it_drt_trip_stats)
    true_labels = merged.waitTime_stats.values/60
    predicted_labels = merged.waitingTime_min_pred.values
    errors = true_labels - predicted_labels
    description = pd.Series(errors).describe()
    # Add mse, rmse, mae
    description['MSE'] = np.mean(errors**2)
    description['RMSE'] = np.sqrt(description['MSE'])
    description['MAE'] = np.mean(np.abs(errors))
    display(pd.DataFrame(description).T)

    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(8, 8), gridspec_kw={'height_ratios': [2, 1]})

    # Scatter plot of actual vs. fitted errors
    ax1.scatter(predicted_labels, errors)
    ax1.axhline(0, color='r', linestyle='--')  # Add a horizontal line at y=0
    ax1.set_xlabel('Predicted wait time (min)')
    ax1.set_ylabel('Actual vs. Predicted Error')
    ax1.set_title('Actual vs. Predicted Wait Time Errors on iteration '+str(iteration))

    # Density plot of errors
    sns.kdeplot(errors, ax=ax2, shade=True, color='red')
    ax2.set_xlabel('Error (min)')
    ax2.set_ylabel('Density')
    ax2.set_title('Error Density Estimation with quantiles (25%, 50%, 75%)')
    #plt.xlim(ax2.get_xlim()[0],100)

    # Add quantiles
    quantiles = np.percentile(errors, [25, 50, 75])
    for q in quantiles:
        ax2.axvline(q, color='navy', linestyle='--', linewidth=1.5)
        #ax2.text(q, 0, f'{q:.2f}', color='g', ha='center', va='top', rotation=90)
    plt.tight_layout()
    plt.show()

def plot_multiple_actual_vs_fitted(plot_list, metric, kde_plot_limit=None, add_iteration_to_title=True):
    """
    plot_list: list of tuples (title, output_dict, iteration)
    """
    if metric not in ['waitTime', 'travelTime']:
        raise ValueError('metric must be either waitTime or travelTime')
    formatted_metric = 'wait time' if metric == 'waitTime' else 'travel time'
    description = pd.DataFrame()
    nrows = len(plot_list)
    ncols = 2
    fig = plt.figure(figsize=(14, 4*nrows), constrained_layout=True)
    fig.suptitle(' ')
    subfigs = fig.subfigures(nrows=nrows, ncols=1)
    #fig, axes = plt.subplots(nrows=nrows, ncols=ncols, figsize=(15, 5*nrows))

    for idx, (title, output_dict, iteration) in enumerate(plot_list):
        it_drt_predictions = output_dict['drt_predictions'][iteration].copy(deep=True)
        it_drt_trip_stats = output_dict['drt_trips_stats'][iteration].copy(deep=True)
        merged = combine_predictions_and_stats(it_drt_predictions,it_drt_trip_stats)
        if metric == 'waitTime':
            true_labels = merged.waitTime_stats.values/60
            predicted_labels = merged.waitingTime_min_pred.values
        else:
            true_labels = merged.totalTravelTime_stats.values/60
            predicted_labels = merged.travelTime_min_pred.values
        errors = true_labels - predicted_labels

        current_description = pd.Series(errors).describe(percentiles=[0.25, 0.5, 0.75, 0.95, 0.99])
        # Add percentiles for abs errors
        abs_errors = np.abs(errors)
        current_description['25% abs error'] = np.percentile(abs_errors, 25)
        current_description['50% abs error'] = np.percentile(abs_errors, 50)
        current_description['75% abs error'] = np.percentile(abs_errors, 75)
        current_description['95% abs error'] = np.percentile(abs_errors, 95)
        current_description['99% abs error'] = np.percentile(abs_errors, 99)
        # Add std of abs errors
        current_description['std abs error'] = np.std(abs_errors)
        # Add mse, rmse, mae
        current_description['MSE'] = np.mean(errors**2)
        current_description['RMSE'] = np.sqrt(current_description['MSE'])
        current_description['MAE'] = np.mean(np.abs(errors))
        # Add percentage of errors below 0
        current_description['% errors < 0 (overestimated)'] = np.sum(errors < 0)/len(errors) * 100
        current_description = pd.DataFrame(current_description).T
        # convert count to int
        current_description['count'] = current_description['count'].astype(int)
        if add_iteration_to_title:
            title = title + ' (it.' + str(iteration) + ')'
        current_description['title'] = title
        description = pd.concat([description, current_description], axis=0)

        subfig = subfigs[idx]
        subfig.suptitle('Errors on ' + title, fontweight='bold', fontsize=14)#, y=1.03)
        axs = subfig.subplots(nrows=1, ncols=2)
        ax_row = idx
        # Scatter plot of actual vs. fitted errors
        #ax = axes[ax_row][0]
        ax = axs[0]
        ax.scatter(predicted_labels, errors)
        ax.axhline(0, color='r', linestyle='--')  # Add a horizontal line at y=0
        ax.set_xlabel('Predicted '+formatted_metric+' (min)', fontsize=12)
        ax.set_ylabel('Actual vs. Predicted Error (min)', fontsize=12)
        ax.set_title('Actual vs. Predicted ' + formatted_metric, fontsize=12)

        # Density plot of errors
        #ax = axes[ax_row][1]
        ax = axs[1]
        sns.kdeplot(errors, ax=ax, shade=True, color='red')
        ax.set_xlabel('Error (min)', fontsize=12)
        ax.set_ylabel('Density', fontsize=12)
        ax.set_title('Error Density Estimation with quantiles (25%, 50%, 75%)', fontsize=12)

        if kde_plot_limit is not None:
            ax.set_xlim(-kde_plot_limit, kde_plot_limit)

        # Add quantiles
        quantiles = np.percentile(errors, [25, 50, 75])
        for q in quantiles:
            ax.axvline(q, color='navy', linestyle='--', linewidth=1.5)
    #plt.tight_layout()
    description = description.set_index('title')
    description.index.name = None
    display(description)
    plt.show()
    return description

def get_confidence_intervals_table(plot_list, metric, confidence_intervals=[2.5,5,7.5,10], add_iteration_to_title=True):
    """
    plot_list: list of tuples (title, output_dict, iteration)
    """
    if metric not in ['waitTime', 'travelTime']:
        raise ValueError('metric must be either waitTime or travelTime')
    formatted_metric = 'wait time' if metric == 'waitTime' else 'travel time'
    description = pd.DataFrame()

    for idx, (title, output_dict, iteration) in enumerate(plot_list):
        it_drt_predictions = output_dict['drt_predictions'][iteration].copy(deep=True)
        it_drt_trip_stats = output_dict['drt_trips_stats'][iteration].copy(deep=True)
        merged = combine_predictions_and_stats(it_drt_predictions,it_drt_trip_stats)
        if metric == 'waitTime':
            true_labels = merged.waitTime_stats.values/60
            predicted_labels = merged.waitingTime_min_pred.values
        else:
            true_labels = merged.totalTravelTime_stats.values/60
            predicted_labels = merged.travelTime_min_pred.values
        errors = true_labels - predicted_labels

        current_description = pd.Series(dtype='float')
        for m in confidence_intervals:
            current_description['% trips with abs error < ' + str(m)] = np.sum(np.abs(errors) < m)/len(errors) * 100


        current_description = pd.DataFrame(current_description).T
        
        if add_iteration_to_title:
            title =  title + ' (it.' + str(iteration) + ')'
        current_description['title'] = title
        description = pd.concat([description, current_description], axis=0)

    description = description.set_index('title')
    description.index.name = None
    return description


def plot_multiple_actual_vs_fitted_only_kde(plot_list, metric, nrows, ncols, kde_plot_limit=None, add_iteration_to_title=True, filename=None):
    """
    plot_list: list of tuples (title, output_dict, iteration)
    """
    if metric not in ['waitTime', 'travelTime']:
        raise ValueError('metric must be either waitTime or travelTime')
    formatted_metric = 'wait time' if metric == 'waitTime' else 'travel time'
    #if len(plot_list) != nrows*ncols:
    #    raise ValueError('Number of plots must be equal to nrows*ncols')
    fig = plt.figure(figsize=(5*ncols, 4*nrows), constrained_layout=True)
    plt.suptitle('Error Density Estimation with quantiles (25%, 50%, 75%)', fontsize=14)

    for idx, (title, output_dict, iteration) in enumerate(plot_list):
        it_drt_predictions = output_dict['drt_predictions'][iteration].copy(deep=True)
        it_drt_trip_stats = output_dict['drt_trips_stats'][iteration].copy(deep=True)
        merged = combine_predictions_and_stats(it_drt_predictions,it_drt_trip_stats)
        if metric == 'waitTime':
            true_labels = merged.waitTime_stats.values/60
            predicted_labels = merged.waitingTime_min_pred.values
        else:
            true_labels = merged.totalTravelTime_stats.values/60
            predicted_labels = merged.travelTime_min_pred.values
        errors = true_labels - predicted_labels

        
        abs_errors = np.abs(errors)
        
        if add_iteration_to_title:
            title = title + ' (it.'+str(iteration) +')'
        plt.subplot(nrows, ncols, idx+1)
        
        # Density plot of errors
        sns.kdeplot(errors, shade=True, color='red')
        plt.xlabel('Error (min)', fontsize=12)
        plt.ylabel('Density', fontsize=12)
        plt.title(title, fontsize=12)

        if kde_plot_limit is not None:
            plt.xlim(-kde_plot_limit, kde_plot_limit)

        # Add quantiles
        quantiles_labels = [25, 50, 75]
        quantiles = np.percentile(errors, quantiles_labels)
        # get current ylim
        ylim = plt.gca().get_ylim()
        for q,t in zip(quantiles, quantiles_labels):
            if q > kde_plot_limit or q < -kde_plot_limit:
                continue
            plt.axvline(q, color='navy', linestyle='--', linewidth=1.5)
            # add text
            plt.text(q - kde_plot_limit*0.06, ylim[1]*0.12, str(t) + '%', color='navy', ha='center', va='top', rotation=90)
    if filename is not None:
        plt.savefig(filename, dpi=300, bbox_inches='tight')
    plt.show()

def plot_actual_vs_fitted_travelTime(data, iteration=-1):
    iteration = len(data['drt_predictions'])-1 if iteration == -1 else iteration
    it_drt_predictions = data['drt_predictions'][iteration].copy(deep=True)
    it_drt_trip_stats = data['drt_trips_stats'][iteration].copy(deep=True)
    merged = combine_predictions_and_stats(it_drt_predictions,it_drt_trip_stats)
    true_labels = merged.totalTravelTime_stats.values/60
    predicted_labels = merged.travelTime_min_pred.values
    errors = true_labels - predicted_labels
    description = pd.Series(errors).describe()
    # Add mse, rmse, mae
    description['MSE'] = np.mean(errors**2)
    description['RMSE'] = np.sqrt(description['MSE'])
    description['MAE'] = np.mean(np.abs(errors))
    display(pd.DataFrame(description).T)

    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(8, 8), gridspec_kw={'height_ratios': [2, 1]})
    # Scatter plot of actual vs. fitted errors
    ax1.scatter(predicted_labels, errors)
    ax1.axhline(0, color='r', linestyle='--')  # Add a horizontal line at y=0
    ax1.set_xlabel('Predicted travel time (min)')
    ax1.set_ylabel('Actual vs. Predicted Error')
    ax1.set_title('Actual vs. Predicted Travel Time Errors on iteration '+str(iteration))

    # Density plot of errors
    sns.kdeplot(errors, ax=ax2, shade=True, color='red')
    ax2.set_xlabel('Error (min)')
    ax2.set_ylabel('Density')
    ax2.set_title('Error Density Estimation with quantiles (25%, 50%, 75%)')
    #plt.xlim(ax2.get_xlim()[0],100)
    
    # Add quantiles
    quantiles = np.percentile(errors, [25, 50, 75])
    for q in quantiles:
        ax2.axvline(q, color='navy', linestyle='--', linewidth=1.5)
        #ax2.text(q, 0, f'{q:.2f}', color='g', ha='center', va='top', rotation=90)
    plt.tight_layout()
    plt.show()

def get_avg_wait_time_per_iteration(drt_trips_stats, start_time_h, end_time_h, field, scale):
    wait_times = []
    for df in drt_trips_stats:
        if start_time_h <= end_time_h:
            df = df[(df.startTime >= start_time_h*3600) & (df.startTime <= end_time_h*3600)]
        else:
            # If end_time_h is smaller than start_time_h, then we have to go through midnight
            df = df[(df.startTime >= start_time_h*3600) | (df.startTime <= end_time_h*3600)]
        wait_times.append(df[field].mean()/scale)
    iterations = np.arange(len(wait_times))
    return iterations, wait_times

def get_ntrips_per_iteration(drt_trips_stats, start_time_h, end_time_h):
    ntrips = []
    for df in drt_trips_stats:
        if start_time_h <= end_time_h:
            df = df[(df.startTime >= start_time_h*3600) & (df.startTime <= end_time_h*3600)]
        else:
            # If end_time_h is smaller than start_time_h, then we have to go through midnight
            df = df[(df.startTime >= start_time_h*3600) | (df.startTime <= end_time_h*3600)]
        ntrips.append(len(df))
    iterations = np.arange(len(ntrips))
    return iterations, ntrips

def plot_iteration_avg_wait_time(plot_list, save=False, filename=None):
    def set_axis_labels_and_title(ax, title, average, legend=False):
        ax.set_xlabel('Iteration', fontsize=14)
        if average:
            ax.set_ylabel('Average Wait Time (min)', fontsize=14)
        else:
            ax.set_ylabel('Number of trips', fontsize=14)
        ax.set_title(title, fontsize=14)
        if legend:
            ax.legend(fontsize=12)
        ax.tick_params(axis='both', which='major', labelsize=12)
    def get_params(predictions):
        if not predictions:
            data_dfs = data['drt_trips_stats']
            field = 'waitTime'
            scale = 60
        else:
            data_dfs = data['drt_predictions']
            field = 'waitingTime_min'
            scale = 1
        return data_dfs, field, scale
    fig, axes = plt.subplots(nrows=6, ncols=3,figsize=(13, 25))#, sharex=True, sharey=True)

    hours_pairs = [(7, 9), (9, 11), (11, 15), (15, 19), (19, 7)]
    for idx, (title, data, color, ls) in enumerate(plot_list):
        data_dfs, field, scale = get_params(False)
        # Plot average wait time per iteration for all times
        iterations, wait_times = get_avg_wait_time_per_iteration(data_dfs, 0, 24, field, scale)
        ax = axes[0][0]
        ax.plot(iterations, wait_times, color=color, ls=ls, label=title)
        set_axis_labels_and_title(ax, 'Average wait time of all times', True, True)

        data_dfs, field, scale = get_params(True)
        iterations, wait_times = get_avg_wait_time_per_iteration(data_dfs, 0, 24, field, scale)
        ax = axes[0][1]
        ax.plot(iterations, wait_times, color=color, ls=ls, label=title)
        set_axis_labels_and_title(ax, 'Average predicted wait time of all times', True)
        ax.sharey(axes[0][0])

        iterations, ntrips = get_ntrips_per_iteration(data['drt_trips_stats'], 0, 24)
        ax = axes[0][2]
        ax.plot(iterations, ntrips, color=color, ls=ls, label=title)
        set_axis_labels_and_title(ax, 'Number of trips of all times', False)
        ax.yaxis.set_major_formatter(mticker.FuncFormatter(lambda x, pos: f'{x / 1000:.0f}K'))


        for index, (start_time_h, end_time_h) in enumerate(hours_pairs, start=1):
            data_dfs, field, scale = get_params(False)
            iterations, wait_times = get_avg_wait_time_per_iteration(data_dfs, start_time_h, end_time_h, field, scale)
            ax = axes[index][0]
            ax.plot(iterations, wait_times, color=color, ls=ls, label=title)
            set_axis_labels_and_title(ax, f'Average wait time of {start_time_h}h-{end_time_h}h', True)
            
            data_dfs, field, scale = get_params(True)
            iterations, wait_times = get_avg_wait_time_per_iteration(data_dfs, start_time_h, end_time_h, field, scale)
            ax = axes[index][1]
            ax.plot(iterations, wait_times, color=color, ls=ls, label=title)
            set_axis_labels_and_title(ax, f'Average predicted wait time of {start_time_h}h-{end_time_h}h', True)
            ax.sharey(axes[index][0])

            iterations, ntrips = get_ntrips_per_iteration(data['drt_trips_stats'], start_time_h, end_time_h)
            ax = axes[index][2]
            ax.plot(iterations, ntrips, color=color, ls=ls, label=title)
            set_axis_labels_and_title(ax, f'Number of trips of {start_time_h}h-{end_time_h}h', False)
            ax.yaxis.set_major_formatter(mticker.FuncFormatter(lambda x, pos: f'{x / 1000:.0f}K'))


    plt.tight_layout()
    if save:
        plt.savefig(filename, dpi=300, bbox_inches='tight')
    plt.show()

def plot_iteration_avg_travel_time(plot_list):
    def set_axis_labels_and_title(ax, title, average):
        ax.set_xlabel('Iteration', fontsize=12)
        if average:
            ax.set_ylabel('Average Travel Time (min)', fontsize=12)
        else:
            ax.set_ylabel('Number of trips', fontsize=12)
        ax.set_title(title, fontsize=12)
        ax.legend()
    def get_params(predictions):
        if not predictions:
            data_dfs = data['drt_trips_stats']
            field = 'totalTravelTime'
            scale = 60
        else:
            data_dfs = data['drt_predictions']
            field = 'travelTime_min'
            scale = 1
        return data_dfs, field, scale
    fig, axes = plt.subplots(nrows=6, ncols=3,figsize=(13, 25))#, sharex=True, sharey=True)

    hours_pairs = [(7, 9), (9, 11), (11, 15), (15, 19), (19, 7)]
    for idx, (title, data, color, ls) in enumerate(plot_list):
        data_dfs, field, scale = get_params(False)
        # Plot average wait time per iteration for all times
        iterations, wait_times = get_avg_wait_time_per_iteration(data_dfs, 0, 24, field, scale)
        ax = axes[0][0]
        ax.plot(iterations, wait_times, color=color, ls=ls, label=title)
        set_axis_labels_and_title(ax, 'Average travel time of all times', True)

        data_dfs, field, scale = get_params(True)
        iterations, wait_times = get_avg_wait_time_per_iteration(data_dfs, 0, 24, field, scale)
        ax = axes[0][1]
        ax.plot(iterations, wait_times, color=color, ls=ls, label=title)
        set_axis_labels_and_title(ax, 'Average predicted travel time of all times', True)
        ax.sharey(axes[0][0])

        iterations, ntrips = get_ntrips_per_iteration(data['drt_trips_stats'], 0, 24)
        ax = axes[0][2]
        ax.plot(iterations, ntrips, color=color, ls=ls, label=title)
        set_axis_labels_and_title(ax, 'Number of trips of all times', False)
        ax.yaxis.set_major_formatter(mticker.FuncFormatter(lambda x, pos: f'{x / 1000:.0f}K'))


        for index, (start_time_h, end_time_h) in enumerate(hours_pairs, start=1):
            data_dfs, field, scale = get_params(False)
            iterations, wait_times = get_avg_wait_time_per_iteration(data_dfs, start_time_h, end_time_h, field, scale)
            ax = axes[index][0]
            ax.plot(iterations, wait_times, color=color, ls=ls, label=title)
            set_axis_labels_and_title(ax, f'Average travel time of {start_time_h}h-{end_time_h}h', True)
            
            data_dfs, field, scale = get_params(True)
            iterations, wait_times = get_avg_wait_time_per_iteration(data_dfs, start_time_h, end_time_h, field, scale)
            ax = axes[index][1]
            ax.plot(iterations, wait_times, color=color, ls=ls, label=title)
            set_axis_labels_and_title(ax, f'Average predicted travel time of {start_time_h}h-{end_time_h}h', True)
            ax.sharey(axes[index][0])

            iterations, ntrips = get_ntrips_per_iteration(data['drt_trips_stats'], start_time_h, end_time_h)
            ax = axes[index][2]
            ax.plot(iterations, ntrips, color=color, ls=ls, label=title)
            set_axis_labels_and_title(ax, f'Number of trips of {start_time_h}h-{end_time_h}h', False)
            ax.yaxis.set_major_formatter(mticker.FuncFormatter(lambda x, pos: f'{x / 1000:.0f}K'))


    plt.tight_layout()
    plt.show()


def plot_iteration_avg_wait_time_only_one(data):
    data_dfs = data['drt_trips_stats']
    field = 'waitTime'
    scale = 60
    iterations, wait_times = get_avg_wait_time_per_iteration(data_dfs, 0, 24, field, scale)
    plt.plot(iterations, wait_times, label='wait time')
    plt.xlabel('Iteration')
    plt.ylabel('Average Wait Time (min)')
    plt.show()