#!/usr/bin/env python
'''Runs import_action on staged .as or .java files.

ln -s /export/who/etc/hooks/pre-commit.py /export/who/.git/hooks/pre-commit

to use it
'''

import os, subprocess, sys

# add aspirin to sys.path so we can import import_action
def add_to_path(*paths):
    parent = os.path.dirname
    here = os.path.abspath(parent(__file__))
    dir = os.path.join(parent(parent(parent(here))), *paths)
    if os.path.exists(dir): sys.path.append(dir)

add_to_path("assemblage", "aspirin", "bin")
add_to_path("aspirin", "bin")

import import_action

def slurp(cmd):
    process = subprocess.Popen(cmd.split(' '), stdout=subprocess.PIPE)
    if process.poll():
        print cmd, "failed, bailing"
        sys.exit(process.returncode)
    return process.communicate()[0].split('\n')

if __name__ == '__main__':
    staged = slurp("git diff --cached --name-only")
    srcfiles = [ln for ln in staged if ln.endswith('.as') or ln.endswith('.java')]
    if not srcfiles:# Nothing to activate on, bail
        sys.exit(0)
    fail = False
    header = open("lib/SOURCE_HEADER").readlines()
    bad_headers = []
    bad_imports = []
    for listing in slurp("git ls-files --stage %s" % " ".join(srcfiles)):
        if listing.strip() == "":
            continue
        sha = listing.split(" ")[1]
        contents = [ln + "\n" for ln in slurp("git show %s" % sha)]
        fn = listing.split("\t")[-1]
        if not import_action.findOrdering(contents)[0]:
            bad_imports.append(fn)
        if header != contents[:len(header)]:
            bad_headers.append(fn)
    if bad_headers or bad_imports:
        print "Header/import issues found. To commit, run the following commands:"
        if bad_headers:
            print "reheader lib/SOURCE_HEADER %s" % ' '.join(bad_headers)
        if bad_imports:
            print "import_action %s" % ' '.join(bad_imports)

        print "git add %s" % ' '.join(set(bad_headers).union(bad_imports))
        sys.exit(1)
