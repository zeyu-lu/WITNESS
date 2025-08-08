import pandas as pd


def sort_txt_file(output_csv_path, txt_file_path):
    # Load the CSV file
    output_df = pd.read_csv(output_csv_path)

    # Load the TXT file into a DataFrame
    # Adjust the separator if your TXT file uses something other than a comma
    txt_df = pd.read_csv(txt_file_path, delimiter=';', header=0)

    # Check if 'MutantNo' and 'TestNo' columns exist in txt_df
    if 'MutantNo' not in txt_df.columns or 'TestNo' not in txt_df.columns:
        raise ValueError("The columns 'MutantNo' and/or 'TestNo' do not exist in the TXT file.")

    # Merge the TXT DataFrame with the CSV DataFrame to align the order
    # The merge operation will sort the TXT DataFrame to match the order in the CSV DataFrame
    sorted_txt_df = pd.merge(output_df, txt_df, on=['MutantNo', 'TestNo'], how='left')

    # Drop additional columns from the merge (if any)
    sorted_txt_df = sorted_txt_df[txt_df.columns]

    return sorted_txt_df


# Path to the CSV and TXT files
output_csv_path = '../jfreechart/chart_5_fixed/myCovMapMtd.csv'
txt_file_path = '../jfreechart/chart_5_fixed/feature_set.txt'
sorted_txt_df = sort_txt_file(output_csv_path, txt_file_path)

# Save the sorted DataFrame back into a TXT file
sorted_txt_file_path = '../jfreechart/chart_5_fixed/feature_data_mtd.txt'
sorted_txt_df.to_csv(sorted_txt_file_path, index=False, sep=';')

print(f"Sorted DataFrame saved to {sorted_txt_file_path}")

