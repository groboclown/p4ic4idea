#!/usr/bin/python3

"""
Generates the file
`src/main/java/com/perforce/p4java/server/IServerMessageCode.java`
based upon the contents of the Perforce C client file
`msgs/msgdm.cc`.
"""

import sys
import os
import re

REL_OUTDIR = "src/main/java/com/perforce/p4java/server"
OUTFILE = "IServerMessageCode.java"
REL_INFILE = "msgs/msgdm.cc"
MSG_PATTERN = re.compile(
    r'\s*ErrorId\s+MsgDm\:\:(\w+)\s*=\s*' +
    r'{\s*ErrorOf\(([^,]+),([^,]+),([^,]+),([^,]+),([^,]+)\),\s*\"(.*?)\"\s*}\s*;'
)

# Map from the msgdm.cc value to the Java equivalent
CONSTANT_MAPPING = {
    "ES_OS": "MessageSubsystemCode.ES_OS",
    "ES_SUPP": "MessageSubsystemCode.ES_SUPP",
    "ES_LBR": "MessageSubsystemCode.ES_LBR",
    "ES_RPC": "MessageSubsystemCode.ES_RPC",
    "ES_DB": "MessageSubsystemCode.ES_DB",
    "ES_DBSUPP": "MessageSubsystemCode.ES_DBSUPP",
    "ES_DM": "MessageSubsystemCode.ES_DM",
    "ES_SERVER": "MessageSubsystemCode.ES_SERVER",
    "ES_CLIENT": "MessageSubsystemCode.ES_CLIENT",
    "ES_INFO": "MessageSubsystemCode.ES_INFO",
    "ES_HELP": "MessageSubsystemCode.ES_HELP",
    "ES_SPEC": "MessageSubsystemCode.ES_SPEC",
    "ES_FTPD": "MessageSubsystemCode.ES_FTPD",
    "ES_BROKER": "MessageSubsystemCode.ES_BROKER",

    "EV_NONE": "MessageGenericCode.EV_NONE",
    "EV_USAGE": "MessageGenericCode.EV_USAGE",
    "EV_UNKNOWN": "MessageGenericCode.EV_UNKNOWN",
    "EV_CONTEXT": "MessageGenericCode.EV_CONTEXT",
    "EV_ILLEGAL": "MessageGenericCode.EV_ILLEGAL",
    "EV_NOTYET": "MessageGenericCode.EV_NOTYET",
    "EV_PROTECT": "MessageGenericCode.EV_PROTECT",
    "EV_EMPTY": "MessageGenericCode.EV_EMPTY",
    "EV_FAULT": "MessageGenericCode.EV_FAULT",
    "EV_CLIENT": "MessageGenericCode.EV_CLIENT",
    "EV_ADMIN": "MessageGenericCode.EV_ADMIN",
    "EV_CONFIG": "MessageGenericCode.EV_CONFIG",
    "EV_UPGRADE": "MessageGenericCode.EV_UPGRADE",
    "EV_COMM": "MessageGenericCode.EV_COMM",
    "EV_TOOBIG": "MessageGenericCode.EV_TOOBIG",

    "E_EMPTY": "MessageSeverityCode.E_EMPTY",
    "E_INFO": "MessageSeverityCode.E_INFO",
    "E_WARN": "MessageSeverityCode.E_WARN",
    "E_FAILED": "MessageSeverityCode.E_FAILED",
    "E_FATAL": "MessageSeverityCode.E_FATAL"
}


def header(out, src):
    out.write("""/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perforce.p4java.server;

import com.perforce.p4java.exception.MessageGenericCode;
import com.perforce.p4java.exception.MessageSubsystemCode;
import com.perforce.p4java.exception.MessageSeverityCode;

/**
 * Error messages loaded from the Perforce C client source.
 * <p>
 * p4ic4idea: DO NOT EDIT.
 * This file is generated from the script {0}
 */
public interface IServerMessageCode {{
""".format(os.path.basename(src)))


def footer(out):
    out.write("""}
""")


def to_java_id(name):
    assert isinstance(name, str)
    ret = ''

    # Special cases...
    if name.startswith('Ex'):
        return 'EX_' + name[2:]
    if name.startswith('Diff2'):
        ret = 'DIFF2'
        name = name[5:]

    last_upper = False
    for c in name:
        if last_upper:
            last_upper = False
        elif c.isupper() or c.isdigit():
            last_upper = True
            if len(ret) > 0:
                ret += '_'
        ret += c.upper()
    return ret


def output_msg_lines(out, msg_lines):
    # Sort the IDs by the message code, so that it's easier to diff changes between
    # clients.
    msg_lines.sort(key=lambda x: int(x[2].strip()))
    for msg in msg_lines:
        output_line(out, *msg)


def output_line(out, name, sub_code, msg_code, severity_code, generic_code, arg_count, text):
    name = name.strip()
    sub_code = sub_code.strip()
    if sub_code in CONSTANT_MAPPING:
        sub_code = CONSTANT_MAPPING[sub_code]
    msg_code = msg_code.strip()
    severity_code = severity_code.strip()
    if severity_code in CONSTANT_MAPPING:
        severity_code = CONSTANT_MAPPING[severity_code]
    generic_code = generic_code.strip()
    if generic_code in CONSTANT_MAPPING:
        generic_code = CONSTANT_MAPPING[generic_code]
    arg_count = arg_count.strip()
    out.write("""
    /**
     * {name}
     * Severity {{@link {severity_code_link}}}
     * Subsystem code {{@link {sub_code_link}}}
     * Generic code {{@link {generic_code_link}}},
     * argument count {arg_count},
     * Text: "{text}"
     */
    int {id} = {msg_code};
""".format(
        name=name,
        id=to_java_id(name),
        sub_code_link=sub_code.replace('.', '#'),
        msg_code=msg_code,
        severity_code_link=severity_code.replace('.', '#'),
        generic_code_link=generic_code.replace('.', '#'),
        arg_count=arg_count,
        text=text
        )
    )


def parse_msgdm(filename):
    ret = []
    with open(filename, 'r') as f:
        lineno = 0
        multiline_comment = False
        buff = ''
        for line in f.readlines():
            lineno += 1
            line = line.strip()
            if len(line) <= 0:
                continue
            if multiline_comment:
                # multiline comment
                p = line.find('*/')
                if p >= 0:
                    line = line[p + 2:].strip()
                    if len(line) > 0:
                        raise Exception("Extra stuff after '*/' ({}), line {}".format(line, lineno))
                    multiline_comment = False
                # Else still inside a multi-line comment
            else:
                p = line.find('/*')
                if p >= 0:
                    line = line[0:p].strip()
                    if len(line) > 0:
                        raise Exception("Extra stuff before '/*', line {}".format(lineno))
                    multiline_comment = True
                    continue
                p = line.find('//')
                if p >= 0:
                    b = line[0:p]
                    a = line[p+2:]
                    if b.count('"') % 2 == 1 and a.count('"') % 2 == 1:
                        # This is a // inside a quoted string.
                        # Not 100% accurate, but good enough.
                        pass
                    else:
                        line = line[0:p].strip()
                        if len(line) <= 0:
                            continue
                if line[0] == '#':
                    # ignore pragma value, and keep current state
                    continue
                buff += line
                # print(">> parsing {}".format(buff))
                m = MSG_PATTERN.match(buff)
                if m:
                    ret.append([
                        m.group(1).strip(), m.group(2).strip(), m.group(3).strip(),
                        m.group(4).strip(), m.group(5).strip(), m.group(6).strip(),
                        m.group(7).strip()
                    ])
                    buff = buff[m.end():]
    return ret


def main(args):
    if len(args) <= 1:
        print("Usage: python3 {} (Perforce c client source directory)".format(args[0]))
        return 1

    base_outdir = os.path.dirname(args[0])
    java_outdir = os.path.join(base_outdir, REL_OUTDIR)
    java_file = os.path.join(java_outdir, OUTFILE)
    c_client_dir = args[1]
    msg_file = os.path.join(c_client_dir, REL_INFILE)

    if not os.path.isdir(java_outdir):
        print("This program is expected to live in the `p4java` directory.")
        return 1

    if not os.path.isfile(msg_file):
        print("{} does not seem to point to the Perforce c client source directory; missing file {}".format(
            args[1], msg_file
        ))
        return 2
    msg_lines = parse_msgdm(msg_file)
    with open(java_file, 'w') as f:
        header(f, args[0])
        output_msg_lines(f, msg_lines)
        footer(f)
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
