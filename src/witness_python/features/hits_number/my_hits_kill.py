import csv
import json
import xml.etree.ElementTree as ET
from pathlib import Path
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor, as_completed


def get_test_dict(workdir):
    test_map_path = Path(workdir) / "testMap.csv"
    test_dict = {}

    with test_map_path.open("r", encoding="utf-8", newline="") as f:
        reader = csv.reader(f)
        next(reader, None)  # skip header
        for row in reader:
            if not row:
                continue
            testno = int(row[0])
            testname = row[1].strip()
            test_dict[testno] = testname

    return test_dict


def get_mutant_dict(workdir):
    mutants_log_path = Path(workdir) / "mutants.log"
    mutant_dict = {}

    with mutants_log_path.open("r", encoding="utf-8") as f:
        for line in f:
            arr = line.strip().split(":")
            if not arr or not arr[0].strip().isdigit():
                continue

            mutantno = int(arr[0].strip())

            ridx = 2
            while ridx <= len(arr):
                candidate = arr[len(arr) - ridx].strip()
                if candidate.isnumeric():
                    linenum = int(candidate)
                    break
                ridx += 1
            else:
                continue

            ridx += 1
            cls = arr[len(arr) - ridx].split("@")[0].strip()

            mutant_dict[mutantno] = (cls, linenum)

    return mutant_dict


def get_classes_instrument(xmlfile):
    tree = ET.parse(xmlfile)
    root = tree.getroot()

    classes_instrument = set()
    for cls in root.findall("./packages/package/classes/class"):
        name = cls.attrib.get("name")
        if name:
            classes_instrument.add(name.strip())

    return classes_instrument


def load_cov_file(workdir, test_name, cls):
    cov_path = Path(workdir) / f"{test_name}_{cls}.txt"

    try:
        with cov_path.open("r", encoding="utf-8") as f:
            covdict = json.load(f)
    except FileNotFoundError:
        return cls, {}
    except json.JSONDecodeError:
        return cls, {}

    return cls, covdict


def build_grouped_requests(workdir, test_dict, mutant_dict, classes_instrument):
    """
    Read covMap.csv once and group requests by test number and class.
    This avoids:
    1. repeatedly reading coverage.xml
    2. repeatedly loading all class files for the same test
    3. nested loops over unrelated classes
    """
    grouped = defaultdict(lambda: defaultdict(list))
    cov_map_path = Path(workdir) / "covMap.csv"

    with cov_map_path.open("r", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            testno = int(row["TestNo"])
            mutantno = int(row["MutantNo"])

            mutant_info = mutant_dict.get(mutantno)
            test_name = test_dict.get(testno)

            if mutant_info is None or test_name is None:
                continue

            cls, linenum = mutant_info

            # Skip classes not instrumented
            if classes_instrument and cls not in classes_instrument:
                continue

            grouped[testno][cls].append((mutantno, linenum))

    return grouped


def process_single_test(workdir, testno, class_requests, test_dict, max_workers=8):
    """
    For one test:
    - load each needed class coverage file once
    - compute hits for all requested mutants of that class
    """
    test_name = test_dict[testno].strip()
    results = []

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        future_to_cls = {
            executor.submit(load_cov_file, workdir, test_name, cls): cls
            for cls in class_requests
        }

        for future in as_completed(future_to_cls):
            cls = future_to_cls[future]
            _, covdict = future.result()

            requests = class_requests[cls]
            for mutantno, linenum in requests:
                hits = covdict.get(str(linenum), 0)
                try:
                    hits = int(hits)
                except (ValueError, TypeError):
                    hits = 0

                if hits < 0:
                    hits = 0

                results.append((testno, mutantno, hits))

    return results


def main():
    workdir = "../commons/csv_1_fixed/"

    workdir_path = Path(workdir)

    test_dict = get_test_dict(workdir)
    mutant_dict = get_mutant_dict(workdir)
    classes_instrument = get_classes_instrument(workdir_path / "coverage.xml")

    grouped_requests = build_grouped_requests(
        workdir, test_dict, mutant_dict, classes_instrument
    )

    output_path = workdir_path / "hitsMap.csv"

    # Process each test once, instead of reloading data for every row in covMap.csv
    with output_path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["TestNo", "MutantNo", "hitsNumber"])

        for testno, class_requests in grouped_requests.items():
            rows = process_single_test(
                workdir=workdir,
                testno=testno,
                class_requests=class_requests,
                test_dict=test_dict,
                max_workers=8,  # adjust based on machine
            )
            writer.writerows(rows)


if __name__ == "__main__":
    main()
