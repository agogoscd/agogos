#!/usr/bin/python

import glob
import os
import yaml

subdir = 'schema'
script_path = os.path.realpath(__file__)
script_dir = os.path.dirname(script_path)
schema_dir = script_dir + '/' + subdir

if not os.path.exists(schema_dir):
    os.makedirs(schema_dir)

for filename in glob.iglob(f'{script_dir}/*.yaml'):
    schema_file = script_dir + '/' + subdir + '/' + os.path.basename(filename).replace('.yaml', '-schema.yaml')

    print('Generate schema file from ' + os.path.basename(filename) + ' => ' + subdir + '/' + os.path.basename(schema_file))

    with open(filename, 'r') as file:
        schema = yaml.full_load(file)

    with open(schema_file, 'w') as file:
        file.write(yaml.dump(schema['spec']['versions'][0]['schema']['openAPIV3Schema']))
