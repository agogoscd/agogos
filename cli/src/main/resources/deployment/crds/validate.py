#!/usr/bin/python

import argparse
import jsonschema
import os
import sys
import yaml

from jsonschema import validate
from yaml import Loader

argParser = argparse.ArgumentParser()
argParser.add_argument("-s", "--schema", required=True, help="YAML schema file used for validation")
argParser.add_argument("-f", "--file", required=True, help="YAML file to be validated against the provided schema")
argParser.add_argument("-q", "--quiet", required=False, action='store_true', help="Suppress all output")
argParser.add_argument("-v", "--verbose", required=False, action='store_true', help="Print detailed validation error information")

args = argParser.parse_args()

with open(args.schema, 'r') as file:
    schema = yaml.load(file, Loader=Loader)

with open(args.file, 'r') as file:
     yfile = yaml.load(file, Loader=Loader)

try:
    validate(yfile, schema)
    if not args.quiet:
        print('File ' + os.path.basename(args.file) + ' validated successfully.')
    sys.exit(0)
except jsonschema.exceptions.ValidationError as e:
    if not args.quiet:
        print('File ' + os.path.basename(args.file) + ' failed validation:')
        if (not args.verbose):
            print('    ' + e.message)
        else:
            print('\n' + str(e))
    sys.exit(1)
