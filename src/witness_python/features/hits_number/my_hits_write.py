import xml.etree.ElementTree as ET
import json
import subprocess
import time
import os
import shutil
import tempfile
from multiprocessing import Pool, cpu_count


def get_covered_tests(workdir):
    cov_map_path = os.path.join(workdir, "covMap.csv")
    test_map_path = os.path.join(workdir, "testMap.csv")

    with open(cov_map_path, "r", encoding="utf-8") as file:
        lines = file.readlines()

    covered_test_nos = set()
    for idx, line in enumerate(lines):
        if idx == 0:
            continue

        line = line.strip()
        if not line:
            continue

        arr = line.split(",")
        test_no = int(arr[1])
        covered_test_nos.add(test_no)

    with open(test_map_path, "r", encoding="utf-8") as file:
        lines = file.readlines()

    testno_to_testname = {}
    for idx, line in enumerate(lines):
        if idx == 0:
            continue

        line = line.strip()
        if not line:
            continue

        arr = line.split(",")
        test_no = int(arr[0])
        test_name = arr[1]
        testno_to_testname[test_no] = test_name

    processed_test_list = []
    for test_no in sorted(covered_test_nos):
        test_name = testno_to_testname.get(test_no)
        if test_name is None:
            print(f"Warning: TestNo {test_no} not found in testMap.csv")
            continue

        result = test_name.index("[")
        processed_test_name = (
            test_name[0:result] + "::" + test_name[result + 1:test_name.rindex("]")]
        )
        processed_test_list.append(processed_test_name)

    return processed_test_list


def ensure_dir(path):
    if not os.path.exists(path):
        os.makedirs(path)


def parse_xml_and_save(result_dir, xmlfile, test):
    tree = ET.parse(xmlfile)
    root = tree.getroot()

    lines_valid = 0
    lines_covered = 0

    for cls in root.findall("./packages/package/classes/class"):
        lines = cls.find("lines")
        if lines is None:
            continue

        covmtx = {}
        for line in lines.findall("line"):
            covmtx[line.attrib["number"]] = int(line.attrib["hits"])

        lines_valid += len(covmtx)

        for value in covmtx.values():
            if value > 0:
                lines_covered += 1

        test_name = test[0:test.index("::")] + "[" + test[test.index("::") + 2:len(test)] + "]"
        output_file = os.path.join(result_dir, f"{test_name}_{cls.attrib['name']}.txt")

        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(covmtx, f)

        time.sleep(3)

    if lines_valid > 0:
        print(f"{test}: {lines_covered}/{lines_valid} = {lines_covered / lines_valid:.4f}")
    else:
        print(f"{test}: no valid lines found")


def run_test_in_isolated_dir(args):
    original_workdir, test, result_dir = args

    temp_parent = tempfile.mkdtemp(prefix="d4j_parallel_")
    isolated_workdir = os.path.join(temp_parent, "project_copy")

    try:
        shutil.copytree(original_workdir, isolated_workdir)

        print(f"Running: defects4j coverage -t {test}")

        process = subprocess.run(
            ["defects4j", "coverage", "-t", test],
            cwd=isolated_workdir,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )

        if process.returncode != 0:
            print(f"Test failed: {test}")
            print(process.stderr)
            return

        xmlfile = os.path.join(isolated_workdir, "coverage.xml")
        if not os.path.exists(xmlfile):
            print(f"coverage.xml not found for test: {test}")
            return

        parse_xml_and_save(result_dir, xmlfile, test)

    except Exception as e:
        print(f"Error while running test {test}: {e}")

    finally:
        shutil.rmtree(temp_parent, ignore_errors=True)


if __name__ == "__main__":
    workdir = "../commons-csv/csv_1_fixed/"
    workdir = os.path.abspath(workdir)

    test_list = get_covered_tests(workdir)
    print("len(test_list):", len(test_list))

    result_dir = os.path.join(workdir, "parallel_coverage_results")
    ensure_dir(result_dir)

    num_processes = max(1, min(cpu_count(), len(test_list)))
    print("num_processes:", num_processes)

    task_args = [(workdir, test, result_dir) for test in test_list]

    with Pool(processes=num_processes) as pool:
        pool.map(run_test_in_isolated_dir, task_args)