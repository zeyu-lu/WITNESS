import pandas as pd


def split_dataset(file_path):
    # Load the data from CSV file
    df = pd.read_csv(file_path, sep=';')

    # Extracting unique values of 'mut_no' as a list
    unique_mut_no = df['MutantNo'].unique().tolist()
    # print("unique_mut_no:", unique_mut_no)

    # Calculating the split index for 90% training data
    split_index = int(len(unique_mut_no) * 0.9)

    # Splitting the unique mut_no into 90% for training and 10% for validation sequentially
    train_mut_no = unique_mut_no[:split_index]
    # print("train_mut_no:", train_mut_no)
    val_mut_no = unique_mut_no[split_index:]
    # print("val_mut_no:", val_mut_no)

    # Creating masks for train and validation sets
    train_mask = df['MutantNo'].isin(train_mut_no)
    val_mask = df['MutantNo'].isin(val_mut_no)

    # Splitting the dataframe into training and validation sets
    df_train = df[train_mask]
    df_val = df[val_mask]

    return df_train, df_val


if __name__ == "__main__":
    workdirs = []
    workdirs.append('../commons-csv/csv_1_fixed/')

    for workdir in workdirs:
        # Replace 'your_file_path' with the actual path to your CSV file
        file_path = workdir + 'feature_data.txt'
        # file_path = workdir + 'feature_data_mtd.txt'

        df_train, df_val = split_dataset(file_path)

        # Saving the split dataframes into new CSV files
        df_train.to_csv(workdir + 'feature_data_train.txt', sep=';', index=False)
        df_val.to_csv(workdir + 'feature_data_val.txt', sep=';', index=False)
        # df_train.to_csv(workdir + 'feature_data_mtd_train.txt', sep=';', index=False)
        # df_val.to_csv(workdir + 'feature_data_mtd_val.txt', sep=';', index=False)
