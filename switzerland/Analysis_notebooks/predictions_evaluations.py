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
    