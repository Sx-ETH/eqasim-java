import pandas as pd
import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt

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

def plot_multiple_actual_vs_fitted(plot_list, metric):
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
        # Add mse, rmse, mae
        current_description['MSE'] = np.mean(errors**2)
        current_description['RMSE'] = np.sqrt(current_description['MSE'])
        current_description['MAE'] = np.mean(np.abs(errors))
        # Add percentage of errors below 0
        current_description['% errors < 0'] = np.sum(errors < 0)/len(errors) * 100
        current_description = pd.DataFrame(current_description).T
        full_title = title + ' on iteration '+str(iteration)
        current_description['title'] = full_title
        description = pd.concat([description, current_description], axis=0)

        subfig = subfigs[idx]
        subfig.suptitle('Errors on ' + full_title, fontweight='bold', fontsize=14)#, y=1.03)
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

        # Add quantiles
        quantiles = np.percentile(errors, [25, 50, 75])
        for q in quantiles:
            ax.axvline(q, color='navy', linestyle='--', linewidth=1.5)
    #plt.tight_layout()
    description = description.set_index('title')
    display(description)
    plt.show()
    return description

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

def plot_iteration_avg_wait_time(plot_list, predictions=False):
    def set_axis_labels_and_title(ax, title):
        ax.set_xlabel('Iteration', fontsize=12)
        ax.set_ylabel('Average Wait Time (min)', fontsize=12)
        ax.set_title(title, fontsize=12)
        ax.legend()

    fig, axes = plt.subplots(nrows=3, ncols=2,figsize=(12, 15))#, sharex=True, sharey=True)
    hours_pairs = [(7, 9), (9, 11), (11, 15), (15, 19), (19, 7)]
    for idx, (title, data, color, ls) in enumerate(plot_list):
        if not predictions:
            data_dfs = data['drt_trips_stats']
            field = 'waitTime'
            scale = 60
        else:
            data_dfs = data['drt_predictions']
            field = 'waitingTime_min'
            scale = 1
        # Plot average wait time per iteration for all times
        iterations, wait_times = get_avg_wait_time_per_iteration(data_dfs, 0, 24, field, scale)
        ax = axes[0][0]
        ax.plot(iterations, wait_times, color=color, ls=ls, label=title)
        set_axis_labels_and_title(ax, 'All times')

        for index, (start_time_h, end_time_h) in enumerate(hours_pairs, start=1):
            ax_row = index//2
            ax_col = index%2
            iterations, wait_times = get_avg_wait_time_per_iteration(data_dfs, start_time_h, end_time_h, field, scale)
            ax = axes[ax_row][ax_col]
            ax.plot(iterations, wait_times, color=color, ls=ls, label=title)
            set_axis_labels_and_title(ax, f'{start_time_h}h-{end_time_h}h')

    plt.tight_layout()
    plt.show()