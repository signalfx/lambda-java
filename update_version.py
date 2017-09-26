import os
import subprocess
from subprocess import PIPE
import sys
import re
import logging

logging.basicConfig(stream=sys.stdout, level=logging.DEBUG)
logger = logging.getLogger(__name__)

version = sys.argv[1]

def execute(cmd, expected_code=None, stdin=None, background=False):
    logger.debug("Executing %s", cmd)
    proc = subprocess.Popen(cmd, stdin=PIPE, stdout=PIPE, stderr=PIPE)
    if background:
        return ('', '', 0) # In background
    stdout, stderr = proc.communicate(stdin)
    logger.debug("Result (%s, %s, %d)", stdout, stderr, proc.returncode)
    ret = (stdout, stderr, proc.returncode)
    if expected_code is not None and expected_code != ret[2]:
        raise ExecuteError("Unable to execute command %s, result %s/%s/%d", ret[0], ret[1], ret[2])
    return ret

(stdout, _, code) = execute(["find", os.getcwd(), '-name', 'pom.xml'], expected_code=0)
base_dir = os.getcwd()
for dir in stdout.strip().split("\n"):
    dirname = os.path.dirname(dir)
    os.chdir(dirname)
    (stdout, _, code) = execute(["mvn", "versions:set", 'versions:update-child-modules', '-DnewVersion=%s' % version], expected_code=0)
    os.chdir(base_dir)