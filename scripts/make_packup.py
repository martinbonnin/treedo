#!/usr/bin/env python

import os
import sys
import csv

if len(sys.argv) < 2:
    sys.stderr.write("usage: %s [directory where to find .csv files]"%sys.argv[0])
    sys.exit(1);

files = []

for f in os.listdir(sys.argv[1]):
    if f.endswith(".csv"):
        files.append(f);

def appendFile(f):
    print f
    fd = open(sys.argv[1] + f)
    reader = csv.reader(fd, delimiter = ',', quotechar = '"');
    rows = []
    for row in reader:
        rows.append(row)

    for i in range(0, len(rows[0])):
        print "    folder " + str(i)
        for row in rows:
            if i < len(row) and row[i] != '':
                print "        " + row[i].lstrip()



for f in files:
    appendFile(f)