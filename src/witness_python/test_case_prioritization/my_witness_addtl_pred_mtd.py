import random
import subprocess


def major_kill_matrix(workdir):
    file = open(workdir + "myCovMapMtd.csv", "r+")
    Lines = file.readlines()

    testset = set()
    mutantset = set()
    num1 = 0
    for line in Lines:
        if num1 > 0:
            arr = line.split(",")
            testno = int(arr[1])
            mutantno = int(arr[0])
            testset.add(testno - 1)
            mutantset.add(mutantno - 1)
        num1 += 1

    file1 = open(workdir + "testMap.csv", "r+")
    Lines1 = file1.readlines()
    rows = len(Lines1) - 1

    file2 = open(workdir + "mutants.log", "r+")
    Lines2 = file2.readlines()
    cols = len(Lines2)

    killmatrix = [[0 for i in range(cols)] for j in range(rows)]

    file3 = open(workdir + "witness_predictions.csv", "r+")
    Lines3 = file3.readlines()

    num = 0
    for line3 in Lines3:
        if num > 0:
            arr = line3.split(",")
            r = int(arr[0])
            r -= 1
            c = int(arr[1])
            c -= 1
            if int(arr[3]) == 1:
                killmatrix[r][c] = 1
        num += 1

    testlist = list(testset)
    mutantlist = list(mutantset)

    return killmatrix, testlist, mutantlist


def mutation_additional(workdir, killmatrix, testlist, mutantlist):

    testmap = {}
    for testno in testlist:
        testmap[testno] = -1073741824.0

    trigger_tests = get_trigger_tests(workdir)

    test_suite = []
    test_set = []
    prevmutsc = -1073741824.0
    while len(testmap) > 0:
        for testno in testmap:
            mytestlist = []
            for test in test_set:
                mytestlist.append(test)
            mytestlist.append(testno)
            mutsc = compute_mutation_score(killmatrix, mytestlist, mutantlist)
            testmap[testno] = mutsc

        sorted_test_pairs = sorted(testmap.items(), key=lambda x: x[1], reverse=True)
        sorted_testmap = {k: v for k, v in sorted_test_pairs}
        print("sorted_testmap:", sorted_testmap)

        if len(sorted_testmap) > 1:
            for index in range(1, len(sorted_testmap)):
                if list(sorted_testmap.values())[index] != list(sorted_testmap.values())[0]:
                    break
        else:
            index = 1

        maxlist = list(sorted_testmap.keys())[:index]
        selected = random.choice(maxlist)
        curmutsc = sorted_testmap.get(selected)

        if curmutsc <= prevmutsc:
            for testno in testmap:
                testmap[testno] = -1073741824.0
            test_suite.extend(test_set)
            test_set = []
            prevmutsc = -1073741824.0
            continue

        testmap.pop(selected)
        test_set.append(selected)

        prevmutsc = curmutsc

        if selected in trigger_tests:
            break

    if len(test_set) > 0:
        test_suite.extend(test_set)
    test_suite.extend(testmap.keys())

    apfd(workdir, test_suite)


# This function is used to compute the mutation score on a subset of kill matrix
# killmatrix: kill matrix (killmatrix[i][j]: the i-th test case can or cannot kill the j-th mutant)
# testlist : (index of )T to compute MS(T,M) e.g. [1,2]
# mutantlist: (index of )M to compute MS(T,M) len(mutantlist)>0, e.g. [1,2]
def compute_mutation_score(killmatrix, testlist, mutantlist):
    newmatrix = []
    for t in testlist:
        tlist = []
        for m in mutantlist:
            tlist.append(killmatrix[t][m])
        newmatrix.append(tlist)
    killed = 0
    for i in range(0, len(mutantlist)):
        x = 0
        for j in range(0, len(testlist)):
            x = x + newmatrix[j][i]
            if x == 1:
                killed = killed + 1
                break
    return float(killed) / len(mutantlist) * 100


def get_test_map(workdir):
    file1 = open(workdir + "testMap.csv", "r+")
    Lines1 = file1.readlines()

    num1 = 0
    test_dict = {}
    for line1 in Lines1:
        if num1 > 0:
            arr = line1.split(",")
            testno = int(arr[0])
            testno -= 1
            testname = arr[1].strip()
            test_dict[testno] = testname
        num1 += 1
    return test_dict


def get_trigger_tests(workdir):
    proc = subprocess.Popen('cat defects4j.build.properties', shell=True, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, cwd=workdir)
    myread = proc.stdout.read()
    myread = str(myread)
    proc.communicate()
    splited = myread.split('d4j.tests.trigger=')
    d4j_tests_trigger = splited[-1][:-3]

    trigger_tests = []
    triggers = d4j_tests_trigger.split(",")
    for trigger in triggers:
        trigger = trigger.replace("::", "[")
        trigger = trigger + "]"
        trigger_tests.append(trigger)

    return trigger_tests


def apfd(workdir, test_suite):
    test_dict = get_test_map(workdir)
    print("test_dict:", test_dict)

    prioritized = []
    for testno in test_suite:
        prioritized.append(test_dict[testno])

    trigger_tests = get_trigger_tests(workdir)

    n = len(prioritized)

    tf1 = n
    cnt = 1
    for test in prioritized:
        if test in trigger_tests:
            tf1 = cnt
            break
        cnt += 1

    res = 1.0 - float(tf1) / n + 1.0 / (2.0 * n)

    file1 = open(workdir + 'witness_addtl_pred.txt', 'a')
    output = "{};{};{};{};{}".format(prioritized, trigger_tests, res, tf1, n)
    file1.write(output + "\n")
    file1.close()


if __name__ == "__main__":

    myworkdir = "../jackson-core/jacksoncore_15_fixed/"
    killmatrix, testlist, mutantlist = major_kill_matrix(myworkdir)

    for i in range(0, 10):
        mutation_additional(myworkdir, killmatrix, testlist, mutantlist)
