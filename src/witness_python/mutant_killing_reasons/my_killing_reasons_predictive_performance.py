import pandas as pd
from sklearn.metrics import precision_score, recall_score, f1_score


# Load the CSV files
workdir = '../gson/gson_15_fixed/'
suffix = 'witness_predictions'
df1 = pd.read_csv(workdir + suffix + '.csv')
df2 = pd.read_csv(workdir + 'killMap.csv')

# Ensure that 'TestNo' and 'MutantNo' are integers in both dataframes
df1['TestNo'] = df1['TestNo'].astype(int)
df1['MutantNo'] = df1['MutantNo'].astype(int)
df2['TestNo'] = df2['TestNo'].astype(int)
df2['MutantNo'] = df2['MutantNo'].astype(int)

# Merge the dataframes based on 'TestNo' and 'MutantNo'
merged_df = pd.merge(df1, df2[['TestNo', 'MutantNo', '[FAIL | TIME | EXC]']], on=['TestNo', 'MutantNo'], how='left')

# Rename the merged column for clarity and fill missing values with 0
merged_df.rename(columns={'[FAIL | TIME | EXC]': 'Reason'}, inplace=True)
merged_df['Reason'].fillna(0, inplace=True)

# Save the merged dataframe to a new CSV file
merged_df.to_csv(workdir + suffix + '_killMap.csv', index=False)

print("Merged file saved as 'merged_output.csv'.")

# Load the merged output CSV file
df = pd.read_csv(workdir + suffix + '_killMap.csv')


# Function to calculate metrics
def calculate_metrics(data, true_col, pred_col):
    precision = precision_score(data[true_col], data[pred_col])
    recall = recall_score(data[true_col], data[pred_col])
    f1 = f1_score(data[true_col], data[pred_col])
    return precision, recall, f1


# Calculate metrics for each category in 'Reason'
categories = ['FAIL', 'TIME', 'EXC']
results = {}
for category in categories:
    # Filter the dataframe for the category
    category_mask = df['Reason'] == category
    # Create binary labels for the category
    df.loc[category_mask, 'binary_label'] = df.loc[category_mask, 'Label'].apply(lambda x: 1 if x == 1 else 0)
    df.loc[category_mask, 'binary_predict'] = df.loc[category_mask, 'predict'].apply(lambda x: 1 if x == 1 else 0)

    # Calculate precision, recall, and f1-score
    filtered_data = df[category_mask]
    precision, recall, f1 = calculate_metrics(filtered_data, 'binary_label', 'binary_predict')
    results[category] = {
        'Precision': precision,
        'Recall': recall,
        'F1 Score': f1
    }

# Output the results
for category, metrics in results.items():
    print(f"{category}:")
    # print(f" Precision: {metrics['Precision']}")
    print(f" Recall: {metrics['Recall']}")
    # print(f" F1 Score: {metrics['F1 Score']}")

