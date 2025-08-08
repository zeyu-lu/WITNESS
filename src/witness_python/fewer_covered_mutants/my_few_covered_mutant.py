import csv


def read_csv_to_multimap(file_path):
    multimap = {}
    unique_test_nos = set()

    with open(file_path, newline='') as csvfile:
        reader = csv.reader(csvfile)
        next(reader)  # Skip the header line

        for mutant_no, test_no in reader:
            mutant_no = int(mutant_no)
            test_no = int(test_no)

            unique_test_nos.add(test_no)
            if mutant_no not in multimap:
                multimap[mutant_no] = []
            multimap[mutant_no].append(test_no)

    return multimap, unique_test_nos


def read_output_csv(file_path):
    output_data = {}

    with open(file_path, newline='') as csvfile:
        reader = csv.reader(csvfile)
        next(reader)  # Skip the header line

        for _, mutant_no, label, predict in reader:
            # mutant_no = int(mutant_no)
            print("mutant_no:", mutant_no)
            mutant_no = int(float(mutant_no))
            if mutant_no not in output_data:
                output_data[mutant_no] = []
            # output_data[mutant_no].append((int(label), int(float(predict))))
            output_data[mutant_no].append((int(float(label)), int(float(predict))))

    return output_data


def calculate_f1_score(output_data):
    tp = sum(label == 1 and predict == 1 for label, predict in output_data)
    fp = sum(label == 0 and predict == 1 for label, predict in output_data)
    fn = sum(label == 1 and predict == 0 for label, predict in output_data)

    precision = tp / (tp + fp) if (tp + fp) > 0 else 0
    recall = tp / (tp + fn) if (tp + fn) > 0 else 0
    f1_score = 2 * (precision * recall) / (precision + recall) if (precision + recall) > 0 else 0

    return f1_score


def process_multimap(multimap, unique_test_nos, output_data):
    overall_tp = 0
    overall_fp = 0
    overall_fn = 0
    ratio_f1_scores = {}

    # Sort the multimap by length of values and then by key
    sorted_multimap = sorted(multimap.items(), key=lambda item: (len(item[1]), item[0]))

    # Process the sorted multimap
    for mutant_no, test_nos in sorted_multimap:
        length = len(test_nos)
        ratio = 0.0 if length == 0 else length / len(unique_test_nos)

        if ratio <= 0.02:
            print("mutant_no:", mutant_no)
            print("test_nos:", test_nos)
            mutant_output_data = output_data.get(mutant_no, [])
            print("mutant_output_data:", mutant_output_data)

            f1_score = calculate_f1_score(mutant_output_data)
            print(f"Mutant No: {mutant_no} (Ratio: {ratio}, F1-Score: {f1_score:.3f})")

            # Update overall TP, FP, and FN
            overall_tp += sum(label == 1 and predict == 1 for label, predict in mutant_output_data)
            overall_fp += sum(label == 0 and predict == 1 for label, predict in mutant_output_data)
            overall_fn += sum(label == 1 and predict == 0 for label, predict in mutant_output_data)

            if ratio not in ratio_f1_scores:
                ratio_f1_scores[ratio] = []
            ratio_f1_scores[ratio].append(f1_score)

    # Compute overall F1-score
    overall_precision = overall_tp / (overall_tp + overall_fp) if (overall_tp + overall_fp) > 0 else 0
    overall_recall = overall_tp / (overall_tp + overall_fn) if (overall_tp + overall_fn) > 0 else 0
    overall_f1_score = 2 * (overall_precision * overall_recall) / (overall_precision + overall_recall) if (overall_precision + overall_recall) > 0 else 0

    print(f"Overall F1-Score for mutants with ratio <= 0.02: {overall_f1_score:.3f}")


def main():
    workdir = "../gson/gson_15_fixed/"
    multimap_path = workdir + "myCovMapMtd.csv"
    output_path = workdir + "witness_predictions.csv"

    multimap, unique_mutant_nos = read_csv_to_multimap(multimap_path)
    output_data = read_output_csv(output_path)
    process_multimap(multimap, unique_mutant_nos, output_data)


if __name__ == "__main__":
    main()
