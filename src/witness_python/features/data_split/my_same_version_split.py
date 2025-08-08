import h2o

# Initialize H2O
h2o.init(max_mem_size="4G")

# Load dataset
workdirs = []
workdirs.append('../commons-csv/csv_1_fixed/')

for workdir in workdirs:
    data_path = workdir + 'feature_data24.txt'
    # data_path = workdir + 'feature_data_mtd24.txt'

    data = h2o.import_file(data_path, sep=';')

    # Splitting logic
    unique_ids = data['MutantNo'].unique().as_data_frame().iloc[:, 0].tolist()
    print("unique_ids:", unique_ids)
    total_ids = len(unique_ids)
    print("total_ids:", total_ids)

    # Calculate set sizes
    train_size = total_ids * 80 // 100
    validation_size = total_ids * 10 // 100
    test_size = total_ids - train_size - validation_size

    train_ids = unique_ids[:train_size]
    validation_ids = unique_ids[train_size:train_size + validation_size]
    test_ids = unique_ids[train_size + validation_size:]

    data_pd = data.as_data_frame()
    train_pd = data_pd[data_pd['MutantNo'].isin(train_ids)]
    validation_pd = data_pd[data_pd['MutantNo'].isin(validation_ids)]
    test_pd = data_pd[data_pd['MutantNo'].isin(test_ids)]
    print("test_pd:", test_pd)

    # Define file paths
    train_path = workdir + 'same-version/train_df.txt'
    validation_path = workdir + 'same-version/validation_df.txt'
    test_path = workdir + 'same-version/test_df.txt'
    # train_path = workdir + 'same-version/train_mtd.txt'
    # validation_path = workdir + 'same-version/validation_mtd.txt'
    # test_path = workdir + 'same-version/test_mtd.txt'

    # Save the DataFrames to txt files with column names
    train_pd.to_csv(train_path, sep=';', index=False)
    validation_pd.to_csv(validation_path, sep=';', index=False)
    test_pd.to_csv(test_path, sep=';', index=False)
