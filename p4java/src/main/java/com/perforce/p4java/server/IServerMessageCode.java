/*
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
 * This file is generated from the script generate-error-codes.py
 */
public interface IServerMessageCode {

    /**
     * CheckFailed
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 2,
     * Text: "%table%/%table2% inconsistencies found."
     */
    int CHECK_FAILED = 1;

    /**
     * InvalidType
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "Invalid %type% '%arg%'."
     */
    int INVALID_TYPE = 2;

    /**
     * IdTooLong
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Identifiers too long.  Must not be longer than 1024 bytes of UTF-8."
     */
    int ID_TOO_LONG = 3;

    /**
     * IdHasDash
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Initial dash character not allowed in '%id%'."
     */
    int ID_HAS_DASH = 4;

    /**
     * IdEmpty
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Empty identifier not allowed."
     */
    int ID_EMPTY = 5;

    /**
     * IdNonPrint
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Non-printable characters not allowed in '%id%'."
     */
    int ID_NON_PRINT = 6;

    /**
     * IdHasRev
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Revision chars (@, #) not allowed in '%id%'."
     */
    int ID_HAS_REV = 7;

    /**
     * IdHasSlash
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Slashes (/) not allowed in '%id%'."
     */
    int ID_HAS_SLASH = 8;

    /**
     * IdNullDir
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Null directory (//) not allowed in '%id%'."
     */
    int ID_NULL_DIR = 9;

    /**
     * IdRelPath
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Relative paths (., ..) not allowed in '%id%'."
     */
    int ID_REL_PATH = 10;

    /**
     * IdWild
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Wildcards (*, %%%%x, ...) not allowed in '%id%'."
     */
    int ID_WILD = 11;

    /**
     * IdNumber
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Purely numeric name not allowed - '%id%'."
     */
    int ID_NUMBER = 12;

    /**
     * BadOption
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "Invalid option '%option%' in %field% option field."
     */
    int BAD_OPTION = 13;

    /**
     * BadChange
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Invalid changelist number '%change%'."
     */
    int BAD_CHANGE = 14;

    /**
     * BadMaxResult
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Invalid maximum value '%value%'."
     */
    int BAD_MAX_RESULT = 15;

    /**
     * BadMaxScanRow
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Invalid %'MaxScanRow'% number '%value%'."
     */
    int BAD_MAX_SCAN_ROW = 16;

    /**
     * BadRevision
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Invalid revision number '%rev%'."
     */
    int BAD_REVISION = 17;

    /**
     * BadTypeMod
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Invalid file type modifier on '%arg%'; see '%'p4 help filetypes'%'."
     */
    int BAD_TYPE_MOD = 18;

    /**
     * BadStorageCombo
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Only one storage modifier +C +D +F or +S allowed on '%arg%'."
     */
    int BAD_STORAGE_COMBO = 19;

    /**
     * BadTypeCombo
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "Disallowed modifier (%option%) on '%arg%'; see '%'p4 help filetypes'%'."
     */
    int BAD_TYPE_COMBO = 20;

    /**
     * BadType
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Invalid file type '%type%'; see '%'p4 help filetypes'%'."
     */
    int BAD_TYPE = 21;

    /**
     * NeedsUpgrades
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 3,
     * Text: "Database is at old upgrade level %level%.  Use '%'p4d -r '%%root%%' -xu'%' to upgrade to level %level2%."
     */
    int NEEDS_UPGRADES = 22;

    /**
     * PastUpgrade
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 2,
     * Text: "Database is at upgrade level %level% past this server's level %level2%."
     */
    int PAST_UPGRADE = 23;

    /**
     * Unicode
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 1,
     * Text: "Database has %value% tables with non-UTF8 text and can't be switched to Unicode mode."
     */
    int UNICODE = 24;

    /**
     * DescMissing
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "%key% description missing!"
     */
    int DESC_MISSING = 25;

    /**
     * NoSuchChange
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "%change% unknown."
     */
    int NO_SUCH_CHANGE = 26;

    /**
     * AlreadyCommitted
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 1,
     * Text: "%change% is already committed."
     */
    int ALREADY_COMMITTED = 27;

    /**
     * WrongClient
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 2,
     * Text: "%change% belongs to client %client%."
     */
    int WRONG_CLIENT = 28;

    /**
     * WrongUser
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 2,
     * Text: "%change% belongs to user %user%."
     */
    int WRONG_USER = 29;

    /**
     * NoSuchCounter
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "No such counter '%counter%'."
     */
    int NO_SUCH_COUNTER = 30;

    /**
     * NotThatCounter
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Too dangerous to touch counter '%counter%'."
     */
    int NOT_THAT_COUNTER = 31;

    /**
     * NoSuchDepot
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "Depot '%depot%' doesn't exist."
     */
    int NO_SUCH_DEPOT = 32;

    /**
     * MaxResults
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DB}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 1,
     * Text: "Request too large (over %maxResults%); see '%'p4 help maxresults'%'."
     */
    int MAX_RESULTS = 32;

    /**
     * NoSuchDepot2
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "Depot '%depot%' unknown - use '%'depot'%' to create it."
     */
    int NO_SUCH_DEPOT_2 = 33;

    /**
     * NoSuchDomain
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 2,
     * Text: "%type% '%name%' doesn't exist."
     */
    int NO_SUCH_DOMAIN = 34;

    /**
     * NoSuchDomain2
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 3,
     * Text: "%type% '%name%' unknown - use '%command%' command to create it."
     */
    int NO_SUCH_DOMAIN_2 = 35;

    /**
     * WrongDomain
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 3,
     * Text: "%name% is a %type%, not a %type2%."
     */
    int WRONG_DOMAIN = 36;

    /**
     * TooManyClients
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Can't add client - over license quota."
     */
    int TOO_MANY_CLIENTS = 37;

    /**
     * NoSuchJob
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "Job '%job%' doesn't exist."
     */
    int NO_SUCH_JOB = 38;

    /**
     * NoSuchJob2
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "Job '%job%' unknown - use 'job' to create it."
     */
    int NO_SUCH_JOB_2 = 39;

    /**
     * NotUnderRoot
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DB}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 2,
     * Text: "Path '%path%' is not under client's root '%root%'."
     */
    int NOT_UNDER_ROOT = 39;

    /**
     * NoSuchFix
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 0,
     * Text: "No such fix."
     */
    int NO_SUCH_FIX = 40;

    /**
     * NotUnderClient
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DB}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 2,
     * Text: "Path '%path%' is not under client '%client%'."
     */
    int NOT_UNDER_CLIENT = 40;

    /**
     * NoPerms
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_PROTECT},
     * argument count 0,
     * Text: "You don't have permission for this operation."
     */
    int NO_PERMS = 41;

    /**
     * PathNotUnder
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 2,
     * Text: "Path '%depotFile%' is not under '%prefix%'."
     */
    int PATH_NOT_UNDER = 42;

    /**
     * TooManyUsers
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Can't create a new user - over license quota."
     */
    int TOO_MANY_USERS = 43;

    /**
     * MapNotUnder
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 2,
     * Text: "Mapping '%depotFile%' is not under '%prefix%'."
     */
    int MAP_NOT_UNDER = 44;

    /**
     * DepotMissing
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Depot %depot% missing from depot table!"
     */
    int DEPOT_MISSING = 45;

    /**
     * DepotVsDomains
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "Depot and domains table out of sync!"
     */
    int DEPOT_VS_DOMAINS = 46;

    /**
     * RevVsRevCx
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "Revision table out of sync with index!"
     */
    int REV_VS_REV_CX = 47;

    /**
     * NoNextRev
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Can't find %depotFile%'s successor rev!"
     */
    int NO_NEXT_REV = 48;

    /**
     * CantFindChange
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Can't find %change%!"
     */
    int CANT_FIND_CHANGE = 49;

    /**
     * BadIntegFlag
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "DmtIntegData unknown DBT_OPEN_FLAG!"
     */
    int BAD_INTEG_FLAG = 50;

    /**
     * BadJobTemplate
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "Job template unusable!"
     */
    int BAD_JOB_TEMPLATE = 51;

    /**
     * NeedJobUpgrade
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Jobs database must be upgraded with '%'p4 jobs -R'%'!"
     */
    int NEED_JOB_UPGRADE = 52;

    /**
     * BadJobPresets
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Presets in jobspec unusable!"
     */
    int BAD_JOB_PRESETS = 53;

    /**
     * JobNameMissing
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Missing job name field!"
     */
    int JOB_NAME_MISSING = 54;

    /**
     * HaveVsRev
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "File %depotFile% isn't in revisions table!"
     */
    int HAVE_VS_REV = 55;

    /**
     * BadOpenFlag
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "DmOpenData unhandled DBT_OPEN_FLAG!"
     */
    int BAD_OPEN_FLAG = 56;

    /**
     * NameChanged
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "File %depotFile% changed it's name!"
     */
    int NAME_CHANGED = 57;

    /**
     * WorkingVsLocked
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "Working and locked tables out of sync!"
     */
    int WORKING_VS_LOCKED = 58;

    /**
     * IntegVsRev
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "%depotFile% is missing from the rev table!"
     */
    int INTEG_VS_REV = 59;

    /**
     * IntegVsWork
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "%depotFile% is missing from the working table!"
     */
    int INTEG_VS_WORK = 60;

    /**
     * AtMostOne
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "In the change being submitted, %depotFile% has more than one 'branch' or 'delete' integration. Use '%'p4 resolved'%' to display the integrations that are contained in this change. Revert any file(s) containing multiple 'branch' or 'delete' integrations. Then re-integrate those files containing at most one such integration. Then retry the submit."
     */
    int AT_MOST_ONE = 61;

    /**
     * MaxScanRows
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DB}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 1,
     * Text: "Too many rows scanned (over %maxScanRows%); see '%'p4 help maxscanrows'%'."
     */
    int MAX_SCAN_ROWS = 61;

    /**
     * MissingDesc
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Change description missing.  You must enter one."
     */
    int MISSING_DESC = 62;

    /**
     * CommandCancelled
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DB}
     * Generic code {@link MessageGenericCode#EV_COMM},
     * argument count 0,
     * Text: "Command terminated because client closed connection."
     */
    int COMMAND_CANCELLED = 62;

    /**
     * BadJobView
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONFIG},
     * argument count 0,
     * Text: "Invalid JobView.  Set with '%'p4 user'%'."
     */
    int BAD_JOB_VIEW = 63;

    /**
     * NoModComChange
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 1,
     * Text: "Can't update committed change %change%."
     */
    int NO_MOD_COM_CHANGE = 64;

    /**
     * TheseCantChange
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 0,
     * Text: "Client and status cannot be changed."
     */
    int THESE_CANT_CHANGE = 65;

    /**
     * CantOpenHere
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 0,
     * Text: "Can't include file(s) not already opened.\nOpen new files with %'p4 add'%, %'p4 edit'%, etc."
     */
    int CANT_OPEN_HERE = 66;

    /**
     * PurgeFirst
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Depot %depot% isn't empty. To delete a depot, all file revisions must be removed and all lazy copy references from other depots must be severed. Use '%'p4 obliterate'%' or '%'p4 snap'%' to break file linkages from other depots, then clear this depot with '%'p4 obliterate'%', then retry the deletion."
     */
    int PURGE_FIRST = 67;

    /**
     * LockedUpdate
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_PROTECT},
     * argument count 3,
     * Text: "Locked %type% '%name%' owned by '%user%'; use -f to force update."
     */
    int LOCKED_UPDATE = 68;

    /**
     * ErrorInSpec
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 1,
     * Text: "Error in %domain% specification."
     */
    int ERROR_IN_SPEC = 69;

    /**
     * LockedDelete
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_PROTECT},
     * argument count 2,
     * Text: "%type% '%name%' is locked and can't be deleted."
     */
    int LOCKED_DELETE = 70;

    /**
     * OpenedDelete
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Client '%client%' has files opened. To delete the client, revert any opened files and delete any pending changes first. An administrator may specify -f to force the delete of another user's client."
     */
    int OPENED_DELETE = 71;

    /**
     * NoSuchGroup
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "Group '%group%' doesn't exist."
     */
    int NO_SUCH_GROUP = 72;

    /**
     * BadMappedFileName
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Branch mapping produced illegal filename."
     */
    int BAD_MAPPED_FILE_NAME = 73;

    /**
     * JobNameJob
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "The job name 'job' is reserved."
     */
    int JOB_NAME_JOB = 74;

    /**
     * JobDescMissing
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "'%field%' field blank.  You must provide it."
     */
    int JOB_DESC_MISSING = 75;

    /**
     * JobFieldReadOnly
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%field% is read-only and can't be changed from '%value%'."
     */
    int JOB_FIELD_READ_ONLY = 77;

    /**
     * MultiWordDefault
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "%field% can't have a default multi-word value."
     */
    int MULTI_WORD_DEFAULT = 78;

    /**
     * JobName101
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "A job name field with code %'101'% must be present."
     */
    int JOB_NAME_10_1 = 79;

    /**
     * LabelOwner
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_PROTECT},
     * argument count 2,
     * Text: "Can't modify label '%label%' owned by '%user%'."
     */
    int LABEL_OWNER = 80;

    /**
     * LabelLocked
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_PROTECT},
     * argument count 1,
     * Text: "Can't modify locked label '%label%'.\nUse 'label' to change label options."
     */
    int LABEL_LOCKED = 81;

    /**
     * WildAdd
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Can't add filenames with wildcards [@#%*] in them.\nUse -f option to force add."
     */
    int WILD_ADD = 82;

    /**
     * UserOrGroup
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Indicator must be 'user' or 'group', not '%value%'."
     */
    int USER_OR_GROUP = 83;

    /**
     * CantChangeUser
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "User name can't be changed from '%user%'."
     */
    int CANT_CHANGE_USER = 84;

    /**
     * Passwd982
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UPGRADE},
     * argument count 0,
     * Text: "You need a 98.2 or newer client to set a password."
     */
    int PASSWD_98_2 = 85;

    /**
     * WrongUserDelete
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_PROTECT},
     * argument count 1,
     * Text: "Not user '%user%'; use -f to force delete."
     */
    int WRONG_USER_DELETE = 86;

    /**
     * DfltBranchView
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "You cannot use the default branch view; it is just a sample."
     */
    int DFLT_BRANCH_VIEW = 87;

    /**
     * FixBadVal
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Job fix status must be one of %values%."
     */
    int FIX_BAD_VAL = 88;

    /**
     * NoClient
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "%clientFile% - can't translate to local path -- no client!"
     */
    int NO_CLIENT = 89;

    /**
     * NoDepot
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Can't find %depot% in depot map!"
     */
    int NO_DEPOT = 90;

    /**
     * NoArchive
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Can't map %lbrFile% to archive!"
     */
    int NO_ARCHIVE = 91;

    /**
     * EmptyRelate
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "RelateMap has empty maps!"
     */
    int EMPTY_RELATE = 92;

    /**
     * BadCaller
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONFIG},
     * argument count 0,
     * Text: "Invalid user (P4USER) or client (P4CLIENT) name."
     */
    int BAD_CALLER = 93;

    /**
     * LockedClient
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_PROTECT},
     * argument count 2,
     * Text: "Locked client '%client%' can only be used by owner '%user%'."
     */
    int LOCKED_CLIENT = 94;

    /**
     * LockedHost
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_PROTECT},
     * argument count 2,
     * Text: "Client '%client%' can only be used from host '%host%'."
     */
    int LOCKED_HOST = 95;

    /**
     * EmptyFileName
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "An empty string is not allowed as a file name."
     */
    int EMPTY_FILE_NAME = 96;

    /**
     * NoRev
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "A revision specification (# or @) cannot be used here."
     */
    int NO_REV = 97;

    /**
     * NoRevRange
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "A revision range cannot be used here."
     */
    int NO_REV_RANGE = 98;

    /**
     * NeedClient
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONFIG},
     * argument count 2,
     * Text: "%arg% - must create client '%client%' to access local files."
     */
    int NEED_CLIENT = 99;

    /**
     * ReferClient
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 2,
     * Text: "%path% - must refer to client '%client%'."
     */
    int REFER_CLIENT = 100;

    /**
     * BadAtRev
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 1,
     * Text: "Invalid changelist/client/label/date '@%arg%'."
     */
    int BAD_AT_REV = 101;

    /**
     * BadRevSpec
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Unintelligible revision specification '%arg%'."
     */
    int BAD_REV_SPEC = 102;

    /**
     * BadRevRel
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Can't yet do relative operations on '%rev%'."
     */
    int BAD_REV_REL = 103;

    /**
     * EmptyResults
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "%reason%."
     */
    int EMPTY_RESULTS = 104;

    /**
     * NoDelete
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "%path% - can't delete remote file!"
     */
    int NO_DELETE = 105;

    /**
     * NoCheckin
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "%path% - can't checkin remote file!"
     */
    int NO_CHECKIN = 106;

    /**
     * RmtError
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "%text%"
     */
    int RMT_ERROR = 107;

    /**
     * TooOld
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UPGRADE},
     * argument count 0,
     * Text: "Remote server is too old to support remote access.  Install a new server."
     */
    int TOO_OLD = 108;

    /**
     * DbFailed
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Remote depot '%depot%' database access failed."
     */
    int DB_FAILED = 109;

    /**
     * ArchiveFailed
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Remote depot '%depot%' archive access failed."
     */
    int ARCHIVE_FAILED = 110;

    /**
     * NoRmtInterop
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Remote depot access is not supported between UNIX and NT prior to 99.2."
     */
    int NO_RMT_INTEROP = 111;

    /**
     * BadTemplate
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 1,
     * Text: "%key% spec template unusable!"
     */
    int BAD_TEMPLATE = 112;

    /**
     * FieldMissing
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Field %field% missing from form."
     */
    int FIELD_MISSING = 113;

    /**
     * NoNotOp
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Can't handle ^ (not) operator there."
     */
    int NO_NOT_OP = 114;

    /**
     * NoCodeZero
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Code %'0'% not allowed on field '%field%'."
     */
    int NO_CODE_ZERO = 115;

    /**
     * SameCode
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "Fields '%field%' and '%field2%' have the same code."
     */
    int SAME_CODE = 116;

    /**
     * NoDefault
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "Field '%field%' needs a preset value to be type '%opt%'."
     */
    int NO_DEFAULT = 117;

    /**
     * SemiInDefault
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Default for '%field%' can't have ;'s in it."
     */
    int SEMI_IN_DEFAULT = 118;

    /**
     * LicensedClients
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 2,
     * Text: "License count: %count% clients used of %max% licensed.\n"
     */
    int LICENSED_CLIENTS = 119;

    /**
     * LicensedUsers
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 2,
     * Text: "License count: %count% users used of %max% licensed.\n"
     */
    int LICENSED_USERS = 120;

    /**
     * TryDelClient
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Try deleting old clients with '%'client -d'%'."
     */
    int TRY_DEL_CLIENT = 121;

    /**
     * TryDelUser
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Try deleting old users with '%'user -d'%'."
     */
    int TRY_DEL_USER = 122;

    /**
     * NoProtect
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_PROTECT},
     * argument count 1,
     * Text: "Access for user '%user%' has not been enabled by '%'p4 protect'%'."
     */
    int NO_PROTECT = 123;

    /**
     * TooManyRoots
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Too many client root alternatives -- only 2 allowed."
     */
    int TOO_MANY_ROOTS = 123;

    /**
     * RmtAuthFailed
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "Remote authorization server access failed."
     */
    int RMT_AUTH_FAILED = 124;

    /**
     * ChangeCreated
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%change% created[ with %workCount% open file(s)][ fixing %jobCount% job(s)]."
     */
    int CHANGE_CREATED = 200;

    /**
     * ChangeUpdated
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%change% updated[, adding %workCount% file(s)][, removing %workCount2% file(s)][, adding %jobCount% fix(es)][, removing %jobCount2% fix(es)]."
     */
    int CHANGE_UPDATED = 201;

    /**
     * ChangeDeleteOpen
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%change% has %count% open file(s) associated with it and can't be deleted."
     */
    int CHANGE_DELETE_OPEN = 202;

    /**
     * ChangeDeleteHasFix
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%change% has %count% fixes associated with it and can't be deleted."
     */
    int CHANGE_DELETE_HAS_FIX = 203;

    /**
     * ChangeDeleteHasFiles
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%change% has %count% files associated with it and can't be deleted."
     */
    int CHANGE_DELETE_HAS_FILES = 204;

    /**
     * ChangeDeleteSuccess
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%change% deleted."
     */
    int CHANGE_DELETE_SUCCESS = 205;

    /**
     * ChangesData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%change% on %date% by %user%@%client%%description%"
     */
    int CHANGES_DATA = 206;

    /**
     * ChangesDataPending
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%change% on %date% by %user%@%client% *pending*%description%"
     */
    int CHANGES_DATA_PENDING = 207;

    /**
     * CountersData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%counterName% = %counterValue%"
     */
    int COUNTERS_DATA = 208;

    /**
     * DirsData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%dirName%"
     */
    int DIRS_DATA = 209;

    /**
     * DepotSave
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Depot %depotName% saved."
     */
    int DEPOT_SAVE = 210;

    /**
     * DepotNoChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Depot %depotName% not changed."
     */
    int DEPOT_NO_CHANGE = 211;

    /**
     * DepotDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Depot %depotName% deleted."
     */
    int DEPOT_DELETE = 212;

    /**
     * DepotsData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 6,
     * Text: "%type% %depotName% %updateDate% %location% %map% '%description%'"
     */
    int DEPOTS_DATA = 213;

    /**
     * DepotsDataExtra
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 7,
     * Text: "%type% %depotName% %updateDate% %location% %address% %map% '%description%'"
     */
    int DEPOTS_DATA_EXTRA = 214;

    /**
     * DescribeChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%change% by %user%@%client% on %date%%description%"
     */
    int DESCRIBE_CHANGE = 215;

    /**
     * DescribeChangePending
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%change% by %user%@%client% on %date% *pending*%description%"
     */
    int DESCRIBE_CHANGE_PENDING = 216;

    /**
     * DescribeData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 3,
     * Text: "%depotFile%%depotRev% %action%"
     */
    int DESCRIBE_DATA = 217;

    /**
     * DescribeDiff
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "\n==== %depotFile%%depotRev% (%type%[/%type2%]) ====\n"
     */
    int DESCRIBE_DIFF = 218;

    /**
     * DiffData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "==== %depotFile%%depotRev% - %localPath% ====[ (%type%)]"
     */
    int DIFF_DATA = 219;

    /**
     * Diff2DataLeft
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "==== %depotFile%%depotRev% - <none> ==="
     */
    int DIFF2_DATA_LEFT = 220;

    /**
     * Diff2DataRight
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "==== <none> - %depotFile%%depotRev% ===="
     */
    int DIFF2_DATA_RIGHT = 221;

    /**
     * Diff2DataContent
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 6,
     * Text: "==== %depotFile%%depotRev% (%type%) - %depotFile2%%depotRev2% (%type2%) ==== content"
     */
    int DIFF2_DATA_CONTENT = 222;

    /**
     * Diff2DataTypes
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 6,
     * Text: "==== %depotFile%%depotRev% (%type%) - %depotFile2%%depotRev2% (%type2%) ==== types"
     */
    int DIFF2_DATA_TYPES = 223;

    /**
     * Diff2DataIdentical
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 6,
     * Text: "==== %depotFile%%depotRev% (%type%) - %depotFile2%%depotRev2% (%type2%) ==== identical"
     */
    int DIFF2_DATA_IDENTICAL = 224;

    /**
     * Diff2DataUnified
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "--- %depotFile%\t%depotDate%\n+++ %depotFile2%\t%depotDate2%"
     */
    int DIFF2_DATA_UNIFIED = 225;

    /**
     * DomainSave
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%domainType% %domainName% saved."
     */
    int DOMAIN_SAVE = 226;

    /**
     * DomainNoChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%domainType% %domainName% not changed."
     */
    int DOMAIN_NO_CHANGE = 227;

    /**
     * DomainDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%domainType% %domainName% deleted."
     */
    int DOMAIN_DELETE = 228;

    /**
     * DomainsDataClient
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%domainType% %domainName% %updateDate% %'root'% %domainMount% '%description%'"
     */
    int DOMAINS_DATA_CLIENT = 229;

    /**
     * DomainsData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%domainType% %domainName% %updateDate% '%description%'"
     */
    int DOMAINS_DATA = 230;

    /**
     * FilelogData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile%"
     */
    int FILELOG_DATA = 231;

    /**
     * FilelogRevDefault
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 4,
     * Text: "%depotRev% %change% %action% on %date%"
     */
    int FILELOG_REV_DEFAULT = 232;

    /**
     * FilelogRevMessage
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 8,
     * Text: "%depotRev% %change% %action% on %date% by %user%@%client% (%type%)%description%"
     */
    int FILELOG_REV_MESSAGE = 233;

    /**
     * FilelogInteg
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 3,
     * Text: "%how% %fromFile%%fromRev%"
     */
    int FILELOG_INTEG = 234;

    /**
     * FilesData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%depotFile%%depotRev% - %action% %change% (%type%)"
     */
    int FILES_DATA = 235;

    /**
     * FixAdd
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%job% fixed by %change% (%status%)."
     */
    int FIX_ADD = 236;

    /**
     * FixAddDefault
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%job% fixed by %change%."
     */
    int FIX_ADD_DEFAULT = 237;

    /**
     * FixDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Deleted fix %job% by %change%."
     */
    int FIX_DELETE = 238;

    /**
     * FixesData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 6,
     * Text: "%job% fixed by %change% on %date% by %user%@%client% (%status%)"
     */
    int FIXES_DATA = 239;

    /**
     * FixesDataDefault
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%job% fixed by %change% on %date% by %user%@%client%"
     */
    int FIXES_DATA_DEFAULT = 240;

    /**
     * GroupCreated
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Group %group% created."
     */
    int GROUP_CREATED = 241;

    /**
     * GroupNotCreated
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Group %group% not created."
     */
    int GROUP_NOT_CREATED = 242;

    /**
     * GroupDeleted
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Group %group% deleted."
     */
    int GROUP_DELETED = 243;

    /**
     * GroupNotUpdated
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Group %group% not updated."
     */
    int GROUP_NOT_UPDATED = 244;

    /**
     * GroupUpdated
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Group %group% updated."
     */
    int GROUP_UPDATED = 245;

    /**
     * GroupsData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%group%"
     */
    int GROUPS_DATA = 246;

    /**
     * HaveData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%haveRev% - %lp%"
     */
    int HAVE_DATA = 247;

    /**
     * InfoUnknownDomain
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%domainType% unknown."
     */
    int INFO_UNKNOWN_DOMAIN = 248;

    /**
     * InfoDomain
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%domainType% root: %root%"
     */
    int INFO_DOMAIN = 249;

    /**
     * IntegAlreadyOpened
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't %action% (already opened on this client)"
     */
    int INTEG_ALREADY_OPENED = 250;

    /**
     * IntegIntoReadOnly
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can only %action% into file in a local depot"
     */
    int INTEG_INTO_READ_ONLY = 251;

    /**
     * IntegXOpened
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't %action% exclusive file already opened"
     */
    int INTEG_XOPENED = 252;

    /**
     * IntegBadAncestor
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%depotFile% - can't %action% from %fromFile%%fromRev% without %'-d'% or %flag% flag"
     */
    int INTEG_BAD_ANCESTOR = 253;

    /**
     * IntegBadBase
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%depotFile% - can't %action% from %fromFile%%fromRev% without -i flag"
     */
    int INTEG_BAD_BASE = 254;

    /**
     * IntegBadAction
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%depotFile% - can't %action% (already opened for %badAction%)[ (remapped from %movedFrom%)]"
     */
    int INTEG_BAD_ACTION = 255;

    /**
     * IntegBadClient
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - is already opened by client %client%"
     */
    int INTEG_BAD_CLIENT = 256;

    /**
     * IntegBadUser
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - is already opened by user %user%"
     */
    int INTEG_BAD_USER = 257;

    /**
     * IntegCantAdd
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't %action% existing file"
     */
    int INTEG_CANT_ADD = 258;

    /**
     * IntegCantModify
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't %action% deleted file"
     */
    int INTEG_CANT_MODIFY = 259;

    /**
     * IntegMustSync
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%toFile% - must sync before integrating."
     */
    int INTEG_MUST_SYNC = 260;

    /**
     * IntegOpenOkay
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%depotFile%%workRev% - %action% from %fromFile%%fromRev%"
     */
    int INTEG_OPEN_OKAY = 261;

    /**
     * IntegSyncBranch
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%depotFile%%workRev% - %action%/%'sync'% from %fromFile%%fromRev%"
     */
    int INTEG_SYNC_BRANCH = 262;

    /**
     * IntegSyncDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%depotFile%%workRev% - %'sync'%/%action% from %fromFile%%fromRev%"
     */
    int INTEG_SYNC_DELETE = 263;

    /**
     * IntegNotHandled
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%workRev% - flag %flag% not handled!"
     */
    int INTEG_NOT_HANDLED = 264;

    /**
     * IntegedData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%toFile%%toRev% - %how% %fromFile%%fromRev%"
     */
    int INTEGED_DATA = 265;

    /**
     * JobSave
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Job %job% saved."
     */
    int JOB_SAVE = 266;

    /**
     * JobNoChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Job %job% not changed."
     */
    int JOB_NO_CHANGE = 267;

    /**
     * JobDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Job %job% deleted."
     */
    int JOB_DELETE = 268;

    /**
     * JobDescription
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%job%[ on %date%][ by %user%][ *%status%*][%description%]"
     */
    int JOB_DESCRIPTION = 269;

    /**
     * EditSpecSave
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Spec %type% saved."
     */
    int EDIT_SPEC_SAVE = 270;

    /**
     * EditSpecNoChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Spec %type% not changed."
     */
    int EDIT_SPEC_NO_CHANGE = 271;

    /**
     * LabelSyncAdd
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%haveRev% - added"
     */
    int LABEL_SYNC_ADD = 272;

    /**
     * LabelSyncDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%haveRev% - deleted"
     */
    int LABEL_SYNC_DELETE = 273;

    /**
     * LabelSyncReplace
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%haveRev% - replaced"
     */
    int LABEL_SYNC_REPLACE = 274;

    /**
     * LabelSyncUpdate
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%haveRev% - updated"
     */
    int LABEL_SYNC_UPDATE = 275;

    /**
     * LockSuccess
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - locking"
     */
    int LOCK_SUCCESS = 276;

    /**
     * LockAlready
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - already locked"
     */
    int LOCK_ALREADY = 277;

    /**
     * LockAlreadyOther
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile% - already locked by %user%@%client%"
     */
    int LOCK_ALREADY_OTHER = 278;

    /**
     * LockNoPermission
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - no permission to lock file"
     */
    int LOCK_NO_PERMISSION = 279;

    /**
     * UnLockSuccess
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - unlocking"
     */
    int UN_LOCK_SUCCESS = 280;

    /**
     * UnLockAlready
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - already unlocked"
     */
    int UN_LOCK_ALREADY = 281;

    /**
     * UnLockAlreadyOther
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile% - locked by %user%@%client%"
     */
    int UN_LOCK_ALREADY_OTHER = 282;

    /**
     * LoggerData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%sequence% %key% %attribute%"
     */
    int LOGGER_DATA = 283;

    /**
     * OpenAlready
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't %action% (already opened on this client)"
     */
    int OPEN_ALREADY = 284;

    /**
     * OpenReadOnly
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can only %action% file in a local depot"
     */
    int OPEN_READ_ONLY = 285;

    /**
     * OpenXOpened
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't %action% exclusive file already opened"
     */
    int OPEN_XOPENED = 286;

    /**
     * OpenBadAction
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile% - can't %action% (already opened for %badAction%)"
     */
    int OPEN_BAD_ACTION = 287;

    /**
     * OpenBadClient
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - is already opened by client %client%"
     */
    int OPEN_BAD_CLIENT = 288;

    /**
     * OpenBadUser
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - is already opened by user %user%"
     */
    int OPEN_BAD_USER = 289;

    /**
     * OpenBadChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't change from %change% - use '%'reopen'%'"
     */
    int OPEN_BAD_CHANGE = 290;

    /**
     * OpenBadType
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't change from %type% - use '%'reopen'%'"
     */
    int OPEN_BAD_TYPE = 291;

    /**
     * OpenReOpen
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%workRev% - reopened for %action%"
     */
    int OPEN_RE_OPEN = 292;

    /**
     * OpenUpToDate
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%workRev% - currently opened for %action%"
     */
    int OPEN_UP_TO_DATE = 293;

    /**
     * OpenCantExists
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't %action% existing file"
     */
    int OPEN_CANT_EXISTS = 294;

    /**
     * OpenCantDeleted
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't %action% deleted file"
     */
    int OPEN_CANT_DELETED = 295;

    /**
     * OpenSuccess
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%workRev% - opened for %action%"
     */
    int OPEN_SUCCESS = 296;

    /**
     * OpenMustResolve
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%depotFile% - must %'sync'%/%'resolve'% %workRev% before submitting"
     */
    int OPEN_MUST_RESOLVE = 297;

    /**
     * OpenIsLocked
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 3,
     * Text: "%depotFile% - locked by %user%@%client%"
     */
    int OPEN_IS_LOCKED = 298;

    /**
     * OpenIsOpened
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 3,
     * Text: "%depotFile% - also opened by %user%@%client%"
     */
    int OPEN_IS_OPENED = 299;

    /**
     * OpenWarnExists
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%depotFile% - warning: %action% of existing file"
     */
    int OPEN_WARN_EXISTS = 300;

    /**
     * OpenWarnDeleted
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%depotFile% - warning: %action% of deleted file"
     */
    int OPEN_WARN_DELETED = 301;

    /**
     * OpenedData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%depotFile%%workRev% - %action% %change% (%type%)"
     */
    int OPENED_DATA = 302;

    /**
     * OpenedOther
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 7,
     * Text: "%depotFile%%workRev% - %action% %change% (%type%) by %user%@%client%"
     */
    int OPENED_OTHER = 303;

    /**
     * OpenedLocked
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%depotFile%%workRev% - %action% %change% (%type%) *locked*"
     */
    int OPENED_LOCKED = 304;

    /**
     * OpenedOtherLocked
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 7,
     * Text: "%depotFile%%workRev% - %action% %change% (%type%) by %user%@%client% *locked*"
     */
    int OPENED_OTHER_LOCKED = 305;

    /**
     * ProtectSave
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "Protections saved."
     */
    int PROTECT_SAVE = 306;

    /**
     * ProtectNoChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "Protections not changed."
     */
    int PROTECT_NO_CHANGE = 307;

    /**
     * PurgeSnapData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%depotFile%%depotRev% - copy from %lbrFile% %lbrRev%"
     */
    int PURGE_SNAP_DATA = 308;

    /**
     * PurgeDeleted
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 6,
     * Text: "Deleted [%onHave% client ][%onLabel% label ][%onInteg% integration ][%onWorking% opened ][%onRev% revision ][and added %synInteg% integration ]record(s)."
     */
    int PURGE_DELETED = 309;

    /**
     * PurgeCheck
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 6,
     * Text: "Would delete [%onHave% client ][%onLabel% label ][%onInteg% integration ][%onWorking% opened ][%onRev% revision ][and add %synInteg% integration ]record(s)."
     */
    int PURGE_CHECK = 310;

    /**
     * PurgeNoRecords
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "No records to delete."
     */
    int PURGE_NO_RECORDS = 311;

    /**
     * PurgeData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%depotRev% - purged"
     */
    int PURGE_DATA = 312;

    /**
     * ReleaseHasPending
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%haveRev% - has pending integrations, not reverted"
     */
    int RELEASE_HAS_PENDING = 313;

    /**
     * ReleaseAbandon
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%haveRev% - was %action%, abandoned"
     */
    int RELEASE_ABANDON = 314;

    /**
     * ReleaseClear
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%haveRev% - was %action%, cleared"
     */
    int RELEASE_CLEAR = 315;

    /**
     * ReleaseDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%haveRev% - was %action%, deleted"
     */
    int RELEASE_DELETE = 316;

    /**
     * ReleaseRevert
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%haveRev% - was %action%, reverted"
     */
    int RELEASE_REVERT = 317;

    /**
     * ReleaseUnlockAbandon
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%haveRev% - was %action%, unlocked and abandoned"
     */
    int RELEASE_UNLOCK_ABANDON = 318;

    /**
     * ReleaseUnlockClear
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%haveRev% - was %action%, unlocked and cleared"
     */
    int RELEASE_UNLOCK_CLEAR = 319;

    /**
     * ReleaseUnlockDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%haveRev% - was %action%, unlocked and deleted"
     */
    int RELEASE_UNLOCK_DELETE = 320;

    /**
     * ReleaseUnlockRevert
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%haveRev% - was %action%, unlocked and reverted"
     */
    int RELEASE_UNLOCK_REVERT = 321;

    /**
     * ReopenData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%depotFile%%workRev% - reopened[; user %user%][; type %type%][; %change%]"
     */
    int REOPEN_DATA = 322;

    /**
     * ReopenDataNoChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%depotFile%%workRev% - nothing changed[; user %user%][; type %type%][; %change%]"
     */
    int REOPEN_DATA_NO_CHANGE = 323;

    /**
     * ResolveDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%localPath% - has been deleted - %'revert'% and %'sync'%."
     */
    int RESOLVE_DELETE = 324;

    /**
     * Resolve2WayRaw
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%localPath% - vs %fromFile%%fromRev%"
     */
    int RESOLVE_2WAY_RAW = 325;

    /**
     * Resolve3WayRaw
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 7,
     * Text: "%localPath% - %baseType%/%headType% merge %fromFile%%fromRev%[ using base %baseFile%][%baseRev%]"
     */
    int RESOLVE_3WAY_RAW = 326;

    /**
     * Resolve3WayText
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%localPath% - merging %fromFile%%fromRev%"
     */
    int RESOLVE_3WAY_TEXT = 327;

    /**
     * ResolvedData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%localPath% - %how% %fromFile%%fromRev%"
     */
    int RESOLVED_DATA = 328;

    /**
     * ReviewData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%change% %user% <%email%> (%fullName%)"
     */
    int REVIEW_DATA = 329;

    /**
     * ReviewsData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%user% <%email%> (%fullname%)"
     */
    int REVIEWS_DATA = 330;

    /**
     * SubmitUpToDate
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - opened at head rev %workRev%"
     */
    int SUBMIT_UP_TO_DATE = 331;

    /**
     * SubmitWasAdd
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - %action% of added file; must %'revert'%"
     */
    int SUBMIT_WAS_ADD = 332;

    /**
     * SubmitWasDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - %action% of deleted file; must %'revert'%"
     */
    int SUBMIT_WAS_DELETE = 333;

    /**
     * SubmitMustResolve
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - must %'resolve'% before submitting"
     */
    int SUBMIT_MUST_RESOLVE = 334;

    /**
     * SubmitTransfer
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%action% %depotFile%%depotRev%"
     */
    int SUBMIT_TRANSFER = 335;

    /**
     * SubmitRefresh
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%depotRev% - refreshing"
     */
    int SUBMIT_REFRESH = 336;

    /**
     * SubmitResolve
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%toFile% - must %'resolve'% %fromFile%%fromRev%"
     */
    int SUBMIT_RESOLVE = 337;

    /**
     * SubmitNewResolve
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%fromFile% - must %'resolve'% %fromRev%"
     */
    int SUBMIT_NEW_RESOLVE = 338;

    /**
     * SubmitChanges
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Use '%'p4 submit -c '%%change%' to submit file(s) in pending %newChange%."
     */
    int SUBMIT_CHANGES = 339;

    /**
     * SyncAdd
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%depotRev% - added as %localPath%"
     */
    int SYNC_ADD = 340;

    /**
     * SyncDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%haveRev% - deleted as %localPath%"
     */
    int SYNC_DELETE = 341;

    /**
     * SyncReplace
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%depotRev% - replacing %localPath%"
     */
    int SYNC_REPLACE = 342;

    /**
     * SyncCantDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%workRev% - is opened for %action% and can't be deleted"
     */
    int SYNC_CANT_DELETE = 343;

    /**
     * SyncCantReplace
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%workRev% - is opened for %action% and can't be replaced"
     */
    int SYNC_CANT_REPLACE = 344;

    /**
     * SyncUpdate
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%depotRev% - updating %localPath%"
     */
    int SYNC_UPDATE = 345;

    /**
     * SyncRefresh
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%depotRev% - refreshing %localPath%"
     */
    int SYNC_REFRESH = 346;

    /**
     * SyncIntegUpdate
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%workRev% - is opened and not being changed"
     */
    int SYNC_INTEG_UPDATE = 347;

    /**
     * SyncIntegDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%workRev% - is opened for %action% - not changed"
     */
    int SYNC_INTEG_DELETE = 348;

    /**
     * SyncIntegBackwards
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%workRev% - is opened at a later revision - not changed"
     */
    int SYNC_INTEG_BACKWARDS = 349;

    /**
     * SyncUptodate
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%haveRev% - is up-to-date"
     */
    int SYNC_UPTODATE = 350;

    /**
     * SyncResolve
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%depotFile% - must %'resolve'% %revRange% before submitting"
     */
    int SYNC_RESOLVE = 351;

    /**
     * TriggerSave
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "Triggers saved."
     */
    int TRIGGER_SAVE = 352;

    /**
     * TriggerNoChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "Triggers not changed."
     */
    int TRIGGER_NO_CHANGE = 353;

    /**
     * TypeMapSave
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%type% saved."
     */
    int TYPE_MAP_SAVE = 354;

    /**
     * TypeMapNoChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%type% not changed."
     */
    int TYPE_MAP_NO_CHANGE = 355;

    /**
     * UserSave
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "User %user% saved."
     */
    int USER_SAVE = 356;

    /**
     * UserNoChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "User %user% not changed."
     */
    int USER_NO_CHANGE = 357;

    /**
     * UserNotExist
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "User %user% doesn't exist."
     */
    int USER_NOT_EXIST = 358;

    /**
     * UserCantDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "User %user% has file(s) open on %value% client(s) and can't be deleted."
     */
    int USER_CANT_DELETE = 359;

    /**
     * UserDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "User %user% deleted."
     */
    int USER_DELETE = 360;

    /**
     * UsersData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%user% <%email%> (%fullName%) accessed %accessDate%"
     */
    int USERS_DATA = 361;

    /**
     * VerifyData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 7,
     * Text: "%depotFile%%depotRev% - %action% %change% (%type%) %digest%[ %status%]"
     */
    int VERIFY_DATA = 362;

    /**
     * StreamTaskAndImport
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Not permitted to update a task stream and Import+ file at the same time '%depotFile%'."
     */
    int STREAM_TASK_AND_IMPORT = 363;

    /**
     * WhereData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%mapFlag%%depotFile% %clientFile% %localPath%"
     */
    int WHERE_DATA = 364;

    /**
     * ExCHANGE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] such changelist."
     */
    int EX_CHANGE = 365;

    /**
     * ExUSER
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] such user(s)."
     */
    int EX_USER = 366;

    /**
     * ExVIEW
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - file(s)|File(s)] not in client view."
     */
    int EX_VIEW = 367;

    /**
     * ExBVIEW
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 2,
     * Text: "%!%[%argc% - no|No] target file(s) in both client and branch view."
     */
    int EX_BVIEW = 368;

    /**
     * ExPROTECT
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] permission for operation on file(s)."
     */
    int EX_PROTECT = 369;

    /**
     * ExPROTNAME
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - protected|Protected] namespace - access denied."
     */
    int EX_PROTNAME = 370;

    /**
     * ExINTEGPEND
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - all|All] revision(s) already integrated in pending changelist."
     */
    int EX_INTEGPEND = 371;

    /**
     * ExINTEGPERM
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - all|All] revision(s) already integrated."
     */
    int EX_INTEGPERM = 372;

    /**
     * ExDIFF
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] differing files."
     */
    int EX_DIFF = 373;

    /**
     * ExDIGESTED
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - file(s)|File(s)] already have digests."
     */
    int EX_DIGESTED = 374;

    /**
     * ExFILE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] such file(s)."
     */
    int EX_FILE = 375;

    /**
     * ExHAVE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - file(s)|File(s)] not on client."
     */
    int EX_HAVE = 376;

    /**
     * ExINTEGED
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] file(s) integrated."
     */
    int EX_INTEGED = 377;

    /**
     * ExLABEL
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - file(s)|File(s)] not in label."
     */
    int EX_LABEL = 378;

    /**
     * ExLABSYNC
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - label|Label] in sync."
     */
    int EX_LABSYNC = 379;

    /**
     * ExOPENALL
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - file(s)|File(s)] not opened anywhere."
     */
    int EX_OPENALL = 380;

    /**
     * ExOPENCHANGE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - file(s)|File(s)] not opened in that changelist."
     */
    int EX_OPENCHANGE = 381;

    /**
     * ExOPENCLIENT
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - file(s)|File(s)] not opened on this client."
     */
    int EX_OPENCLIENT = 382;

    /**
     * ExOPENNOTEDIT
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - file(s)|File(s)] not opened for edit."
     */
    int EX_OPENNOTEDIT = 383;

    /**
     * ExOPENDFLT
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - file(s)|File(s)] not opened in default changelist."
     */
    int EX_OPENDFLT = 384;

    /**
     * ExRESOLVED
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] file(s) resolved."
     */
    int EX_RESOLVED = 385;

    /**
     * ExTOINTEG
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] file(s) to integrate."
     */
    int EX_TOINTEG = 386;

    /**
     * ExTORESOLVE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] file(s) to resolve."
     */
    int EX_TORESOLVE = 387;

    /**
     * ExUPTODATE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - file(s)|File(s)] up-to-date."
     */
    int EX_UPTODATE = 388;

    /**
     * ExABOVECHANGE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] revision(s) above those at that changelist number."
     */
    int EX_ABOVECHANGE = 389;

    /**
     * ExABOVEDATE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] revision(s) after that date."
     */
    int EX_ABOVEDATE = 390;

    /**
     * ExABOVEHAVE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] revision(s) above those on client."
     */
    int EX_ABOVEHAVE = 391;

    /**
     * ExABOVELABEL
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] revision(s) above those in label."
     */
    int EX_ABOVELABEL = 392;

    /**
     * ExABOVEREVISION
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] revision(s) above that revision."
     */
    int EX_ABOVEREVISION = 393;

    /**
     * ExATCHANGE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] file(s) at that changelist number."
     */
    int EX_ATCHANGE = 394;

    /**
     * ExATDATE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] file(s) as of that date."
     */
    int EX_ATDATE = 395;

    /**
     * ExATHAVE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - file(s)|File(s)] not on client."
     */
    int EX_ATHAVE = 396;

    /**
     * ExATLABEL
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - file(s)|File(s)] not in label."
     */
    int EX_ATLABEL = 397;

    /**
     * ExATREVISION
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] file(s) at that revision."
     */
    int EX_ATREVISION = 398;

    /**
     * ExATACTION
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] file(s) with that action."
     */
    int EX_ATACTION = 399;

    /**
     * ExBELOWCHANGE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] revision(s) below those at that changelist number."
     */
    int EX_BELOWCHANGE = 400;

    /**
     * ExBELOWDATE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] revision(s) before that date."
     */
    int EX_BELOWDATE = 401;

    /**
     * ExBELOWHAVE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] revision(s) below those on client."
     */
    int EX_BELOWHAVE = 402;

    /**
     * ExBELOWLABEL
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] revision(s) below those in label."
     */
    int EX_BELOWLABEL = 403;

    /**
     * ExBELOWREVISION
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] revision(s) below that revision."
     */
    int EX_BELOWREVISION = 404;

    /**
     * OpenWarnPurged
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%depotFile% - warning: %action% of purged file"
     */
    int OPEN_WARN_PURGED = 405;

    /**
     * MonitorData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 9,
     * Text: "%id% [%prog% ][%host% ][%runstate%|T] %user% [%client% ]%elapsed% %function% %args%"
     */
    int MONITOR_DATA = 406;

    /**
     * MonitorClear
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "** process '%id%' record cleared **"
     */
    int MONITOR_CLEAR = 407;

    /**
     * MonitorTerminate
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "** process '%id%' marked for termination **"
     */
    int MONITOR_TERMINATE = 408;

    /**
     * MonitorCantTerminate
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "** process '%id%' can't terminate, runtime < 10 seconds **"
     */
    int MONITOR_CANT_TERMINATE = 409;

    /**
     * LameCodes
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 3,
     * Text: "Field codes must be between %low%-%hi% for %type% specs."
     */
    int LAME_CODES = 410;

    /**
     * ProtectedCodes
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Builtin field %code% cannot be changed."
     */
    int PROTECTED_CODES = 411;

    /**
     * BadSpecType
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Unknown spec type %type%."
     */
    int BAD_SPEC_TYPE = 412;

    /**
     * SameTag
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "Fields '%field%' and '%field2%' have the same tag."
     */
    int SAME_TAG = 413;

    /**
     * SpecSave
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Spec %type% saved."
     */
    int SPEC_SAVE = 414;

    /**
     * SpecNoChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Spec %type% not changed."
     */
    int SPEC_NO_CHANGE = 415;

    /**
     * SpecDeleted
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Spec %type% deleted."
     */
    int SPEC_DELETED = 416;

    /**
     * SpecNotDefined
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "Spec %type% not defined."
     */
    int SPEC_NOT_DEFINED = 417;

    /**
     * BadDigest
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Invalid digest string '%digest%'."
     */
    int BAD_DIGEST = 418;

    /**
     * NoClearText
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Passwords can only be set by 'p4 passwd' at this security level."
     */
    int NO_CLEAR_TEXT = 419;

    /**
     * DepotSpecDup
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 1,
     * Text: "There is already a %'spec'% depot called '%depot%'."
     */
    int DEPOT_SPEC_DUP = 420;

    /**
     * NoIntegOverlays
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Overlay (+) mappings are not allowed in branch views."
     */
    int NO_INTEG_OVERLAYS = 421;

    /**
     * BadTimeout
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Invalid Timeout value '%value%'."
     */
    int BAD_TIMEOUT = 422;

    /**
     * Diff2DataRightPre041
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "==== < none > - %depotFile%%depotRev% ===="
     */
    int DIFF2_DATA_RIGHT_PRE_04_1 = 423;

    /**
     * IntegOpenOkayBase
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 8,
     * Text: "%depotFile%%workRev% - %action% from %fromFile%%fromRev%[ using base %baseFile%][%baseRev%][ (remapped from %remappedFrom%)]"
     */
    int INTEG_OPEN_OKAY_BASE = 424;

    /**
     * Resolve3WayTextBase
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%localPath% - merging %fromFile%%fromRev% using base %baseFile%%baseRev%"
     */
    int RESOLVE_3WAY_TEXT_BASE = 425;

    /**
     * BadRevPend
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Can't use a pending changelist number for this command."
     */
    int BAD_REV_PEND = 426;

    /**
     * IntegSyncIntegBase
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 8,
     * Text: "%depotFile%%workRev% - %'sync'%/%action% from %fromFile%%fromRev%[ using base %baseFile%][%baseRev%][ (remapped from %remappedFrom%)]"
     */
    int INTEG_SYNC_INTEG_BASE = 427;

    /**
     * ResolvedDataBase
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 6,
     * Text: "%localPath% - %how% %fromFile%%fromRev% base %baseFile%%baseRev%"
     */
    int RESOLVED_DATA_BASE = 428;

    /**
     * TraitCleared
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%depotRev% - %name% cleared"
     */
    int TRAIT_CLEARED = 429;

    /**
     * TraitNotSet
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%depotRev% - %name% not set"
     */
    int TRAIT_NOT_SET = 430;

    /**
     * TraitSet
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%depotRev% - %name% set"
     */
    int TRAIT_SET = 431;

    /**
     * MapNoListAccess
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 0,
     * Text: "User does not have list access for mapped depots."
     */
    int MAP_NO_LIST_ACCESS = 432;

    /**
     * InvalidEscape
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Target file has illegal escape sequence [%xx]."
     */
    int INVALID_ESCAPE = 433;

    /**
     * JobHasChanged
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 0,
     * Text: "Job has been modified by another user, clear date field to overwrite."
     */
    int JOB_HAS_CHANGED = 434;

    /**
     * BadTypePartial
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "A partial file type is not allowed here."
     */
    int BAD_TYPE_PARTIAL = 435;

    /**
     * ManyRevSpec
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Too many revision specifications (max %max%)."
     */
    int MANY_REV_SPEC = 436;

    /**
     * LabelNoSync
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 0,
     * Text: "The Revision field can only be added to empty labels."
     */
    int LABEL_NO_SYNC = 437;

    /**
     * LabelHasRev
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Label '%label%' has a Revision field and must remain empty."
     */
    int LABEL_HAS_REV = 438;

    /**
     * TwistedMap
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_TOOBIG},
     * argument count 0,
     * Text: "Client map too twisted for directory list."
     */
    int TWISTED_MAP = 439;

    /**
     * ProtectsData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 6,
     * Text: "%perm% %isgroup% %user% %ipaddr% %mapFlag%%depotFile%"
     */
    int PROTECTS_DATA = 440;

    /**
     * JobFieldAlways
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%field% is a read-only always field and can't be changed from '%value%'.\nThe [%spec%|job] may have been updated while you were editing."
     */
    int JOB_FIELD_ALWAYS = 441;

    /**
     * DepotMapInvalid
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "%'Map'% entry '%map%' must have only 1 wildcard which must be a trailing '/...' or '\\...'."
     */
    int DEPOT_MAP_INVALID = 442;

    /**
     * ReleaseNotOwner
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%haveRev% - belongs to user %user%, not reverted"
     */
    int RELEASE_NOT_OWNER = 443;

    /**
     * SubmitReverted
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%depotRev% - unchanged, reverted"
     */
    int SUBMIT_REVERTED = 444;

    /**
     * SubmitMovedToDefault
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%depotRev% - unchanged, moved to default changelist"
     */
    int SUBMIT_MOVED_TO_DEFAULT = 445;

    /**
     * FilesSummary
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%path% %fileCount% files %fileSize% bytes [%blockCount% blocks]"
     */
    int FILES_SUMMARY = 446;

    /**
     * PendingDelete
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Client '%client%' has pending changes. To delete the client, delete any pending changes first. An administrator may specify -f to force the delete of another user's client."
     */
    int PENDING_DELETE = 447;

    /**
     * NoIntegHavemaps
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Havemap entries found in non-client view!"
     */
    int NO_INTEG_HAVEMAPS = 448;

    /**
     * IntegTooVirtual
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't %action% file branched with %'integrate -v'%"
     */
    int INTEG_TOO_VIRTUAL = 449;

    /**
     * LicenseSave
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "License file saved."
     */
    int LICENSE_SAVE = 450;

    /**
     * LicenseNoChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "License file not changed."
     */
    int LICENSE_NO_CHANGE = 451;

    /**
     * ProtectsMaxData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%perm%"
     */
    int PROTECTS_MAX_DATA = 452;

    /**
     * MaxLockTime
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 1,
     * Text: "Operation took too long (over %maxLockTime% seconds); see '%'p4 help maxlocktime'%'."
     */
    int MAX_LOCK_TIME = 453;

    /**
     * FilesDiskUsage
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%depotFile%%depotRev% %fileSize% bytes [%blockCount% blocks]"
     */
    int FILES_DISK_USAGE = 454;

    /**
     * LabelLoop
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Too many automatic labels (label '%label%' may refer to itself)."
     */
    int LABEL_LOOP = 455;

    /**
     * ProtectsEmpty
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Protections table is empty."
     */
    int PROTECTS_EMPTY = 456;

    /**
     * SyncCantPublishIsOpen
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - can't %'sync -p'% a file that's opened"
     */
    int SYNC_CANT_PUBLISH_IS_OPEN = 457;

    /**
     * SyncCantPublishOnHave
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - can't %'sync -p'% a file that's synced"
     */
    int SYNC_CANT_PUBLISH_ON_HAVE = 458;

    /**
     * RetypeData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%depotFile%%depotRev% - %oldType% now %newType%"
     */
    int RETYPE_DATA = 459;

    /**
     * BadVersionCount
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Bad version count '+S%count%', only values 1-10,16,32,64,128,256,512 allowed."
     */
    int BAD_VERSION_COUNT = 460;

    /**
     * IntegIncompatable
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - file type of %fromFile% incompatible"
     */
    int INTEG_INCOMPATABLE = 461;

    /**
     * DupOK
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%file%%rev% duplicated from %file2%%rev2%"
     */
    int DUP_OK = 462;

    /**
     * DupExists
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%file%%rev% already exists"
     */
    int DUP_EXISTS = 463;

    /**
     * DupLocked
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%file%%rev% is opened for %action% on client %client%"
     */
    int DUP_LOCKED = 464;

    /**
     * JobDeleteHasFix
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%job% has %count% fixes associated with it and can't be deleted."
     */
    int JOB_DELETE_HAS_FIX = 465;

    /**
     * AdminSpecData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - created"
     */
    int ADMIN_SPEC_DATA = 466;

    /**
     * OwnerCantChange
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 0,
     * Text: "Client, user, date and status cannot be changed."
     */
    int OWNER_CANT_CHANGE = 467;

    /**
     * ChangeNotOwner
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%change% can only be updated by user %user%."
     */
    int CHANGE_NOT_OWNER = 468;

    /**
     * ProtectsNoSuper
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Can't delete last valid 'super' entry from protections table."
     */
    int PROTECTS_NO_SUPER = 469;

    /**
     * TryEvalLicense
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "For additional licenses, contact Perforce Sales at sales@perforce.com."
     */
    int TRY_EVAL_LICENSE = 470;

    /**
     * Diff2DataUnifiedDiffer
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "Binary files %depotFile%%depotRev% and %depotFile2%%depotRev2% differ"
     */
    int DIFF2_DATA_UNIFIED_DIFFER = 471;

    /**
     * GroupNotOwner
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "User '%user%' is not an owner of group '%group%'."
     */
    int GROUP_NOT_OWNER = 472;

    /**
     * OpenFilesCantChange
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 0,
     * Text: "Change has files open, client cannot be modified."
     */
    int OPEN_FILES_CANT_CHANGE = 473;

    /**
     * GroupsDataVerbose
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%group% %maxresults% %maxscanrows% %maxtimeout% %timeout%"
     */
    int GROUPS_DATA_VERBOSE = 474;

    /**
     * AdminLockDataEx
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 1,
     * Text: "Write: %name%"
     */
    int ADMIN_LOCK_DATA_EX = 475;

    /**
     * AdminLockDataSh
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 1,
     * Text: "Read : %name%"
     */
    int ADMIN_LOCK_DATA_SH = 476;

    /**
     * ExVIEW2
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 2,
     * Text: "%!%[%argc% - file(s)|File(s)] not in client view."
     */
    int EX_VIEW2 = 477;

    /**
     * ExSVIEW
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] source file(s) in branch view."
     */
    int EX_SVIEW = 478;

    /**
     * ExTVIEW
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 2,
     * Text: "%!%[%argc% - no|No] target file(s) in branch view."
     */
    int EX_TVIEW = 479;

    /**
     * ExPROTECT2
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 2,
     * Text: "%!%[%argc% - no|No] permission for operation on file(s)."
     */
    int EX_PROTECT2 = 480;

    /**
     * ExPROTNAME2
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 2,
     * Text: "%!%[%argc% - protected|Protected] namespace - access denied."
     */
    int EX_PROTNAME2 = 481;

    /**
     * RmtSequenceFailed
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Remote '%counter%' counter update failed."
     */
    int RMT_SEQUENCE_FAILED = 482;

    /**
     * OutOfSequence
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 2,
     * Text: "Sequence error:  local 'change' counter '%local%' vs remote '%remote%'!"
     */
    int OUT_OF_SEQUENCE = 483;

    /**
     * ChangeExists
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Sequence error:  next changelist '%change%' already exists!"
     */
    int CHANGE_EXISTS = 484;

    /**
     * IdHasComma
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Commas (,) not allowed in '%id%'."
     */
    int ID_HAS_COMMA = 485;

    /**
     * ReleaseHasMoved
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%haveRev% - has been moved, not reverted"
     */
    int RELEASE_HAS_MOVED = 486;

    /**
     * MoveSuccess
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%toFile%%toRev% - moved from %fromFile%%fromRev%"
     */
    int MOVE_SUCCESS = 487;

    /**
     * MoveBadAction
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't move (already opened for %badAction%)"
     */
    int MOVE_BAD_ACTION = 488;

    /**
     * MoveExists
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - can't move to an existing file"
     */
    int MOVE_EXISTS = 489;

    /**
     * MoveMisMatch
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "Mismatched move on client!"
     */
    int MOVE_MIS_MATCH = 490;

    /**
     * MoveNoMatch
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile% - needs %direction%file %movedFile%"
     */
    int MOVE_NO_MATCH = 491;

    /**
     * SyncMissingMoveSource
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't sync moved file,  %fromFile% is missing from the rev table!"
     */
    int SYNC_MISSING_MOVE_SOURCE = 492;

    /**
     * OpenWarnMoved
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%depotFile% - warning: %action% of moved file"
     */
    int OPEN_WARN_MOVED = 493;

    /**
     * MoveReadOnly
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - can't move to a spec or remote depot"
     */
    int MOVE_READ_ONLY = 494;

    /**
     * DescribeMove
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 4,
     * Text: "%depotFile%%depotRev% moved from %depotFile2%%depotRev2%"
     */
    int DESCRIBE_MOVE = 495;

    /**
     * FieldCount
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "'%tag%' unknown or wrong number of fields for path-type."
     */
    int FIELD_COUNT = 496;

    /**
     * StreamNotOwner
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Stream '%stream%' is owned by '%owner%'."
     */
    int STREAM_NOT_OWNER = 497;

    /**
     * StreamsData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "Stream %stream% %type% %parent% '%title%'[ %status%]"
     */
    int STREAMS_DATA = 498;

    /**
     * StreamRootErr
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Stream '%stream%' must begin with '%'//'%'."
     */
    int STREAM_ROOT_ERR = 499;

    /**
     * MaxOpenFiles
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 1,
     * Text: "Opening too many files (over %maxOpenFiles%); see '%'p4 help maxopenfiles'%'."
     */
    int MAX_OPEN_FILES = 500;

    /**
     * StreamNested
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Streams cannot be nested. '%stream%' contains existing stream '%nested%'."
     */
    int STREAM_NESTED = 501;

    /**
     * StreamDoubleSlash
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Stream '%stream%' contains embedded double slashes (%'//'%)."
     */
    int STREAM_DOUBLE_SLASH = 502;

    /**
     * StreamEqDepot
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Stream '%stream%' must be below depot level."
     */
    int STREAM_EQ_DEPOT = 503;

    /**
     * StreamDepthErr
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Stream '%stream%' must reside in first folder below depot level."
     */
    int STREAM_DEPTH_ERR = 504;

    /**
     * StreamEndSlash
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Trailing slash not allowed in '%id%'."
     */
    int STREAM_END_SLASH = 505;

    /**
     * StreamVsDomains
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "Stream and domains table out of sync!"
     */
    int STREAM_VS_DOMAINS = 506;

    /**
     * DepotNotStream
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Depot %'type'% for '%depot%' must be '%'stream'%'."
     */
    int DEPOT_NOT_STREAM = 507;

    /**
     * ExSTREAM
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] such stream."
     */
    int EX_STREAM = 508;

    /**
     * OpenFilesCantChangeUser
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 0,
     * Text: "Change has files open, user cannot be modified."
     */
    int OPEN_FILES_CANT_CHANGE_USER = 509;

    /**
     * UnshelveBadAction
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't unshelve (already opened for %badAction%)"
     */
    int UNSHELVE_BAD_ACTION = 510;

    /**
     * UnshelveSuccess
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%depotRev% - unshelved, opened for %action%"
     */
    int UNSHELVE_SUCCESS = 511;

    /**
     * ShelveCantUpdate
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "%depotFile% - already shelved, use %'-f'% to update."
     */
    int SHELVE_CANT_UPDATE = 512;

    /**
     * UnshelveIsLocked
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "File(s) in this shelve are locked - try again later!"
     */
    int UNSHELVE_IS_LOCKED = 513;

    /**
     * LocWild
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%loc% wildcards (*, ...) not allowed in path: '%path%'."
     */
    int LOC_WILD = 514;

    /**
     * PosWild
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Positional wildcards (%%%%x) not allowed in path: '%path%'."
     */
    int POS_WILD = 515;

    /**
     * ExTOUNSHELVE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] file(s) to unshelve."
     */
    int EX_TOUNSHELVE = 516;

    /**
     * ChangeDeleteShelved
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 1,
     * Text: "%change% has shelved files associated with it and can't be deleted."
     */
    int CHANGE_DELETE_SHELVED = 517;

    /**
     * ShelveLocked
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - shelved file locked, try again later."
     */
    int SHELVE_LOCKED = 518;

    /**
     * ShelveUnlocked
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - shelved file unlocked, try again later."
     */
    int SHELVE_UNLOCKED = 519;

    /**
     * UnshelveBadAdd
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Can't unshelve %depotFile% to open for %badAdd%: file already exists."
     */
    int UNSHELVE_BAD_ADD = 520;

    /**
     * UnshelveBadEdit
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Can't unshelve %depotFile% to open for %badEdit%: file does not exist or has been deleted."
     */
    int UNSHELVE_BAD_EDIT = 521;

    /**
     * ShelveIncompatible
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "%depotFile% - can't overwrite a shelved moved file, use %'-r'% to replace."
     */
    int SHELVE_INCOMPATIBLE = 522;

    /**
     * ShelveMaxFiles
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Shelve file limit exceeded (over %maxFiles%)."
     */
    int SHELVE_MAX_FILES = 523;

    /**
     * ShelvedDelete
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Client '%client%' has files shelved; use '%'shelve -df'%' to remove them, and then try again,\nor use '%'client -df -Fs'%' to delete the client and leave the shelved changes intact."
     */
    int SHELVED_DELETE = 524;

    /**
     * LockBadUnicode
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - cannot submit unicode type file using non-unicode server"
     */
    int LOCK_BAD_UNICODE = 525;

    /**
     * LockUtf16NotSupp
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - utf16 files can not be submitted by pre-2007.2 clients"
     */
    int LOCK_UTF_16_NOT_SUPP = 526;

    /**
     * ExDIFFPre101
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] file(s) to diff."
     */
    int EX_DIFFPre101 = 527;

    /**
     * MoveNotSynced
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - not synced, can't force move"
     */
    int MOVE_NOT_SYNCED = 528;

    /**
     * MoveNotResolved
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - is unresolved, can't force move"
     */
    int MOVE_NOT_RESOLVED = 529;

    /**
     * MoveNeedForce
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%clientFile% - is synced; use -f to force move"
     */
    int MOVE_NEED_FORCE = 530;

    /**
     * GrepOutput
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%depotFile%#%depotRev%%separator%%linecontent%"
     */
    int GREP_OUTPUT = 531;

    /**
     * GrepFileOutput
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%#%depotRev%"
     */
    int GREP_FILE_OUTPUT = 532;

    /**
     * GrepWithLineNumber
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 6,
     * Text: "%depotFile%#%depotRev%%separator1%%linenumber%%separator2%%linecontent%"
     */
    int GREP_WITH_LINE_NUMBER = 533;

    /**
     * GrepLineTooLong
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%depotFile%#%depotRev% - line %linenumber%: maximum line length of %maxlinelength% exceeded"
     */
    int GREP_LINE_TOO_LONG = 534;

    /**
     * GrepMaxRevs
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Grep revision limit exceeded (over %maxRevs%)."
     */
    int GREP_MAX_REVS = 535;

    /**
     * GrepSeparator
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "--"
     */
    int GREP_SEPARATOR = 536;

    /**
     * NoSuchRelease
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "There was no Perforce release named '%release%'."
     */
    int NO_SUCH_RELEASE = 537;

    /**
     * ClientTooOld
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "You may not set %'minClient'% to a release newer than your client."
     */
    int CLIENT_TOO_OLD = 538;

    /**
     * SubmitWasDeleteCanReadd
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - %action% of deleted file; must %'sync'% & %'add -d'% or %'revert'%"
     */
    int SUBMIT_WAS_DELETE_CAN_READD = 539;

    /**
     * ExATTEXT
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] file(s) of type %'text'%."
     */
    int EX_ATTEXT = 540;

    /**
     * ShelveNoPerm
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - no permission to shelve file"
     */
    int SHELVE_NO_PERM = 541;

    /**
     * InvalidParent
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 1,
     * Text: "Invalid parent field '%parent%'. Check stream, parent and type."
     */
    int INVALID_PARENT = 542;

    /**
     * EmbWild
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Embedded wildcards (*, ...) not allowed in '%path%'."
     */
    int EMB_WILD = 543;

    /**
     * ImportNotUnder
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 1,
     * Text: "Import path '%depotFile%' is not under an accessible depot."
     */
    int IMPORT_NOT_UNDER = 544;

    /**
     * ConfigData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%serverName%: %variableName% = %variableValue%"
     */
    int CONFIG_DATA = 545;

    /**
     * NoSuchConfig
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "No such configuration variable '%config%'."
     */
    int NO_SUCH_CONFIG = 546;

    /**
     * ConfigWasNotSet
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "Configuration variable '%config%' did not have a value."
     */
    int CONFIG_WAS_NOT_SET = 547;

    /**
     * UseConfigure
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 0,
     * Text: "Usage: { %'set [name#]var=value | unset [name#]var'% }"
     */
    int USE_CONFIGURE = 548;

    /**
     * StreamOverflow
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Stream hierarchy in endless loop!"
     */
    int STREAM_OVERFLOW = 549;

    /**
     * NoStreamAtChange
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 2,
     * Text: "No stream '%stream%' existed at change %change%"
     */
    int NO_STREAM_AT_CHANGE = 550;

    /**
     * IntegMovedUnmapped
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - not in client view (remapped from %movedFrom%)"
     */
    int INTEG_MOVED_UNMAPPED = 551;

    /**
     * IntegMovedNoAccess
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - no permission (remapped from %movedFrom%)"
     */
    int INTEG_MOVED_NO_ACCESS = 552;

    /**
     * OpenWarnOpenStream
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "%depotFile% - warning: cannot submit from non-stream client"
     */
    int OPEN_WARN_OPEN_STREAM = 553;

    /**
     * OpenWarnOpenNotStream
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%depotFile% - warning: cannot submit from stream %stream% client"
     */
    int OPEN_WARN_OPEN_NOT_STREAM = 554;

    /**
     * UsersDataLong
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 7,
     * Text: "%user% <%email%> (%fullName%) accessed %accessDate% type %type% ticket expires %endDate% password last changed %passDate%"
     */
    int USERS_DATA_LONG = 555;

    /**
     * IntegMovedOutScope
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - not in branch view (remapped from %movedFrom%)"
     */
    int INTEG_MOVED_OUT_SCOPE = 556;

    /**
     * NotStreamReady
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Client '%client%' requires an application that can fully support streams."
     */
    int NOT_STREAM_READY = 557;

    /**
     * NotBucket
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "%depot% is not an archive depot."
     */
    int NOT_BUCKET = 558;

    /**
     * BucketAdd
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "Archiving %depotFile%%depotRev% to %archiveFile%."
     */
    int BUCKET_ADD = 559;

    /**
     * BucketRestore
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "Restoring %depotFile%%depotRev% from %archiveFile%."
     */
    int BUCKET_RESTORE = 560;

    /**
     * BucketPurge
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Purged %depotFile%%depotRev%."
     */
    int BUCKET_PURGE = 561;

    /**
     * BucketSkipHead
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Not archiving %depotFile%%depotRev%: head revision."
     */
    int BUCKET_SKIP_HEAD = 562;

    /**
     * BucketSkipLazy
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Not archiving %depotFile%%depotRev%: lazy copy."
     */
    int BUCKET_SKIP_LAZY = 563;

    /**
     * BucketSkipBranched
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Not archiving %depotFile%%depotRev%: content used elsewhere."
     */
    int BUCKET_SKIP_BRANCHED = 564;

    /**
     * BucketSkipType
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Not archiving %depotFile%%depotRev%: stored in delta format (+D)."
     */
    int BUCKET_SKIP_TYPE = 565;

    /**
     * BucketNoFilesToArchive
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "[%argc% - no|No] revisions can be archived."
     */
    int BUCKET_NO_FILES_TO_ARCHIVE = 566;

    /**
     * BucketNoFilesToRestore
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "[%argc% - no|No] revisions can be restored."
     */
    int BUCKET_NO_FILES_TO_RESTORE = 567;

    /**
     * BucketNoFilesToPurge
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "[%argc% - no|No] revisions can be purged."
     */
    int BUCKET_NO_FILES_TO_PURGE = 568;

    /**
     * CopyOpenTarget
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 0,
     * Text: "Can't copy to target path with files already open."
     */
    int COPY_OPEN_TARGET = 569;

    /**
     * OpenWarnFileNotMapped
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%depotFile% - warning: file not mapped in stream %stream% client"
     */
    int OPEN_WARN_FILE_NOT_MAPPED = 570;

    /**
     * NotAsService
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONFIG},
     * argument count 0,
     * Text: "Command not allowed for a service user."
     */
    int NOT_AS_SERVICE = 571;

    /**
     * AnnotateTooBig
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_TOOBIG},
     * argument count 1,
     * Text: "File size exceeds %'dm.annotate.maxsize'% (%maxSize% bytes)."
     */
    int ANNOTATE_TOO_BIG = 572;

    /**
     * CommittedNoPerm
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%change% can only be updated by user %user% with -u, or by admin user with -f."
     */
    int COMMITTED_NO_PERM = 573;

    /**
     * PendingNoPerm
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%change% can only be updated by user %user%, or by admin user with -f."
     */
    int PENDING_NO_PERM = 574;

    /**
     * MissingStream
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 2,
     * Text: "Missing stream '%name%' in stream hierarchy for '%stream%'."
     */
    int MISSING_STREAM = 575;

    /**
     * InvalidStreamFmt
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Stream '%stream%' is not the correct format of '//depotname/string'"
     */
    int INVALID_STREAM_FMT = 576;

    /**
     * CantChangeUserType
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "User type can't be changed."
     */
    int CANT_CHANGE_USER_TYPE = 577;

    /**
     * StreamPathRooted
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "View '%view%' must be relative and not contain leading slashes"
     */
    int STREAM_PATH_ROOTED = 578;

    /**
     * StreamPathSlash
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Imported path '%view%' requires leading slashes in full depot path"
     */
    int STREAM_PATH_SLASH = 579;

    /**
     * StreamHasChildren
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Stream '%stream%' has child streams; cannot delete until they are removed."
     */
    int STREAM_HAS_CHILDREN = 580;

    /**
     * StreamHasClients
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Stream '%stream%' has %type% clients; cannot delete until they are removed."
     */
    int STREAM_HAS_CLIENTS = 581;

    /**
     * StreamOwnerReq
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Owner field of Stream '%stream%' required."
     */
    int STREAM_OWNER_REQ = 582;

    /**
     * StreamIncompatibleP
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 4,
     * Text: "Stream '%stream%' (%type%) not compatible with Parent %parent% (%parentType%); use -u to force update."
     */
    int STREAM_INCOMPATIBLE_P = 583;

    /**
     * StreamIncompatibleC
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 3,
     * Text: "Stream '%stream%' (%oldType% -> %type%) not compatible with child streams; use -u to force update."
     */
    int STREAM_INCOMPATIBLE_C = 584;

    /**
     * BucketSkipBucketed
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Not archiving %depotFile%%depotRev%: trait '%'archiveBucket'%' has been set."
     */
    int BUCKET_SKIP_BUCKETED = 585;

    /**
     * StreamOwnerUpdate
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Stream '%stream%' owner '%owner%' required for -u force update."
     */
    int STREAM_OWNER_UPDATE = 586;

    /**
     * ProtectsNotCompatible
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Helix P4Admin tool not compatible with '##' comments in protection table.\nIf you wish to continue using Helix P4Admin to administer the protection table please remove all '##' comments."
     */
    int PROTECTS_NOT_COMPATIBLE = 587;

    /**
     * ResolveAction
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 6,
     * Text: "%localPath% - resolving %resolveType% from %fromFile%%fromRev%[ using base %baseFile%][%baseRev%]"
     */
    int RESOLVE_ACTION = 590;

    /**
     * ResolveFiletype
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "Filetype resolve"
     */
    int RESOLVE_FILETYPE = 591;

    /**
     * ResolveFiletypeAction
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "(%type%)"
     */
    int RESOLVE_FILETYPE_ACTION = 592;

    /**
     * ResolvedAction
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 6,
     * Text: "%localPath% - resolved %resolveType% from %fromFile%%fromRev%[ using base %baseFile%][%baseRev%]"
     */
    int RESOLVED_ACTION = 593;

    /**
     * ResolveDoBranch
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "Branch resolve"
     */
    int RESOLVE_DO_BRANCH = 594;

    /**
     * ResolveDoBranchActionT
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "branch"
     */
    int RESOLVE_DO_BRANCH_ACTION_T = 595;

    /**
     * ResolveDoBranchActionY
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "ignore"
     */
    int RESOLVE_DO_BRANCH_ACTION_Y = 596;

    /**
     * ResolveDoDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "Delete resolve"
     */
    int RESOLVE_DO_DELETE = 597;

    /**
     * ResolveDoDeleteActionT
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "delete"
     */
    int RESOLVE_DO_DELETE_ACTION_T = 598;

    /**
     * ResolveDoDeleteActionY
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "ignore"
     */
    int RESOLVE_DO_DELETE_ACTION_Y = 599;

    /**
     * ResolveMove
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "Filename resolve"
     */
    int RESOLVE_MOVE = 600;

    /**
     * ResolveMoveAction
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotPath%"
     */
    int RESOLVE_MOVE_ACTION = 601;

    /**
     * SyncNotSafeAdd
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%depotRev% - can't overwrite existing file %localPath%"
     */
    int SYNC_NOT_SAFE_ADD = 602;

    /**
     * SyncNotSafeDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%depotRev% - can't delete modified file %localPath%"
     */
    int SYNC_NOT_SAFE_DELETE = 603;

    /**
     * SyncNotSafeUpdate
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%depotRev% - can't update modified file %localPath%"
     */
    int SYNC_NOT_SAFE_UPDATE = 604;

    /**
     * SyncNotSafeReplace
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%depotRev% - can't replace modified file %localPath%"
     */
    int SYNC_NOT_SAFE_REPLACE = 605;

    /**
     * SyncIndexOutOfBounds
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 2,
     * Text: "Index out of range %index% of %total%"
     */
    int SYNC_INDEX_OUT_OF_BOUNDS = 606;

    /**
     * OpenedSwitch
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Client '%client%' has files opened; use -f to force switch."
     */
    int OPENED_SWITCH = 607;

    /**
     * DomainSwitch
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%domainType% %domainName% switched."
     */
    int DOMAIN_SWITCH = 608;

    /**
     * MonitorPause
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "** process '%id%' record paused **"
     */
    int MONITOR_PAUSE = 609;

    /**
     * MonitorResume
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "** process '%id%' record resumed **"
     */
    int MONITOR_RESUME = 610;

    /**
     * UnknownReplicationMode
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Unknown replication mode '%mode%'."
     */
    int UNKNOWN_REPLICATION_MODE = 611;

    /**
     * UnknownReplicationTarget
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Unknown replication target '%target%'."
     */
    int UNKNOWN_REPLICATION_TARGET = 612;

    /**
     * DiskSpaceMinimum
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 3,
     * Text: "The filesystem '%filesys%' has only %freeSpace% free, but the server configuration requires at least %cfgSpace% available."
     */
    int DISK_SPACE_MINIMUM = 613;

    /**
     * DiskSpaceEstimated
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 3,
     * Text: "The filesystem '%filesys%' has only %freeSpace% free, but the command estimates it needs at least %estSpace% available."
     */
    int DISK_SPACE_ESTIMATED = 614;

    /**
     * MayNotBeNegative
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Negative value not allowed for counter '%counter%'."
     */
    int MAY_NOT_BE_NEGATIVE = 615;

    /**
     * NoPrevRev
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Unable to show the differences for file %depotFile% because the revision prior to %depotRev% is missing from the revisions table. If the prior revisions of this file were obliterated, this is a normal situation; otherwise it may indicate repository damage and should be investigated further."
     */
    int NO_PREV_REV = 616;

    /**
     * MustBeNumeric
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Non-numeric value not allowed for counter '%counter%'."
     */
    int MUST_BE_NUMERIC = 617;

    /**
     * NoDepotTypeChange
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 0,
     * Text: "Depot %'type'% cannot be changed."
     */
    int NO_DEPOT_TYPE_CHANGE = 618;

    /**
     * SnapFirst
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Depot %depot% isn't empty of archive contents. One or more files are still present in the depot directory. Other depots may have branched or integrated from files in this depot. Break those linkages with '%'p4 snap'%' and/or remove references from other depots with '%'p4 obliterate'%' first. Next, remove all non-Perforce files from the depot prior to depot deletion. Once all references have been removed, either remove any remaining physical files and directories from the depot directory and retry the operation, or specify '-f' to bypass this check and delete the depot."
     */
    int SNAP_FIRST = 619;

    /**
     * ShelveNeedsResolve
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile% - must %'resolve'% %file%%srev% before shelving"
     */
    int SHELVE_NEEDS_RESOLVE = 620;

    /**
     * UnshelveResolve
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 3,
     * Text: "%depotFile% - must %'resolve'% %fromFile%%rev% before submitting"
     */
    int UNSHELVE_RESOLVE = 621;

    /**
     * ResolveActionMove
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%localPath% - resolving move to %fromFile%[ using base %baseFile%]"
     */
    int RESOLVE_ACTION_MOVE = 622;

    /**
     * ResolvedActionMove
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%localPath% - resolved move (%how% %fromFile%)[ using base %baseFile%]"
     */
    int RESOLVED_ACTION_MOVE = 623;

    /**
     * BadTypeAuto
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Automatic type detection is not allowed here."
     */
    int BAD_TYPE_AUTO = 624;

    /**
     * LimitBadArg
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "%path% - must refer to a local depot in depot syntax."
     */
    int LIMIT_BAD_ARG = 625;

    /**
     * MoveDeleted
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%clientFile% - can't move (open for %action%); must accept other resolve(s) or ignore"
     */
    int MOVE_DELETED = 626;

    /**
     * DeleteMoved
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%clientFile% - can't delete moved file; must undo move first"
     */
    int DELETE_MOVED = 627;

    /**
     * ResourceAlreadyLocked
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 2,
     * Text: "Resource %objType%#%objName% is already locked!"
     */
    int RESOURCE_ALREADY_LOCKED = 628;

    /**
     * NoSuchResource
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 2,
     * Text: "Resource %objType%#%objName% was never locked!"
     */
    int NO_SUCH_RESOURCE = 629;

    /**
     * StreamIsMainline
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Stream '%name%' has no parent, therefore (command not allowed)."
     */
    int STREAM_IS_MAINLINE = 630;

    /**
     * IntegVsShelve
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 2,
     * Text: "%depotFile%%rev%: can't resolve (shelved change was deleted); must %'revert'%, or %'revert -k'% and edit before submit."
     */
    int INTEG_VS_SHELVE = 631;

    /**
     * OperatorNotAllowed
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_PROTECT},
     * argument count 2,
     * Text: "Operator user %userName% may not perform operation %funcName%"
     */
    int OPERATOR_NOT_ALLOWED = 632;

    /**
     * ShelveOpenResolves
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile% - unshelved file for [%user%@]%client% needs %'resolve'%"
     */
    int SHELVE_OPEN_RESOLVES = 633;

    /**
     * OpenedXData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%depotFile%%workRev% - %action% %change% (%type%) *exclusive*"
     */
    int OPENED_XDATA = 635;

    /**
     * OpenedXOther
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 7,
     * Text: "%depotFile%%workRev% - %action% %change% (%type%) by %user%@%client% *exclusive*"
     */
    int OPENED_XOTHER = 636;

    /**
     * WildAddTripleDots
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Can't add filenames containing the ellipsis wildcard (...)."
     */
    int WILD_ADD_TRIPLE_DOTS = 637;

    /**
     * MergeBadBase
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile% - can't merge unrelated file %fromFile%%fromRev%"
     */
    int MERGE_BAD_BASE = 638;

    /**
     * ResolveShelveDelete
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 2,
     * Text: "%depotFile%%rev%: can't resolve (shelved file was deleted); must %'revert'%, or %'revert -k'% and edit before submit."
     */
    int RESOLVE_SHELVE_DELETE = 639;

    /**
     * ConcurrentFileChange
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "%depotFile% was altered during the course of the submit. Verify the contents of the changelist and retry the submit."
     */
    int CONCURRENT_FILE_CHANGE = 640;

    /**
     * UnshelveBadClientView
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't unshelve (already opened for %action% using a different client view path)"
     */
    int UNSHELVE_BAD_CLIENT_VIEW = 641;

    /**
     * IdHasPercent
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Percent character not allowed in '%id%'."
     */
    int ID_HAS_PERCENT = 642;

    /**
     * NoSuchKey
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "No such key '%key%'."
     */
    int NO_SUCH_KEY = 643;

    /**
     * OpenedDataS
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile% - %action% %change%"
     */
    int OPENED_DATA_S = 644;

    /**
     * OpenedOtherS
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%depotFile% - %action% %change% by %user%@%client%"
     */
    int OPENED_OTHER_S = 645;

    /**
     * OpenedLockedS
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile% - %action% %change% *locked*"
     */
    int OPENED_LOCKED_S = 646;

    /**
     * OpenedOtherLockedS
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%depotFile% - %action% %change% by %user%@%client% *locked*"
     */
    int OPENED_OTHER_LOCKED_S = 647;

    /**
     * OpenedXDataS
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile% - %action% %change% *exclusive*"
     */
    int OPENED_XDATA_S = 648;

    /**
     * OpenedXOtherS
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%depotFile% - %action% %change% by %user%@%client% *exclusive*"
     */
    int OPENED_XOTHER_S = 649;

    /**
     * ExUNLOCKCHANGE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - shelved|Shelved] file(s) not locked in that changelist."
     */
    int EX_UNLOCKCHANGE = 650;

    /**
     * RmtJournalWaitFailed
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "Remote %'journalwait'% failed!"
     */
    int RMT_JOURNAL_WAIT_FAILED = 651;

    /**
     * ServerSave
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Server %serverName% saved."
     */
    int SERVER_SAVE = 661;

    /**
     * ServerNoChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Server %serverName% not changed."
     */
    int SERVER_NO_CHANGE = 662;

    /**
     * ServerDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Server %serverName% deleted."
     */
    int SERVER_DELETE = 663;

    /**
     * NoSuchServer
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "Server '%server%' doesn't exist."
     */
    int NO_SUCH_SERVER = 664;

    /**
     * ServersData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 6,
     * Text: "%serverID% %type% %name% %address% %services% '%description%'"
     */
    int SERVERS_DATA = 665;

    /**
     * ResolveTrait
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "Attribute resolve"
     */
    int RESOLVE_TRAIT = 666;

    /**
     * ResolveTraitActionT
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "overwrite your open attributes with their set of [%count% ]attribute(s)"
     */
    int RESOLVE_TRAIT_ACTION_T = 667;

    /**
     * ResolveTraitActionY
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "leave your set of [%count% ]open attribute(s) unchanged"
     */
    int RESOLVE_TRAIT_ACTION_Y = 668;

    /**
     * ResolveTraitActionM
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "merge their [%count% ]propagating attribute(s) with your set of attribute(s)"
     */
    int RESOLVE_TRAIT_ACTION_M = 669;

    /**
     * UnidentifiedServer
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Server identity has not been defined, use '%'p4d -xD'%' to specify it."
     */
    int UNIDENTIFIED_SERVER = 670;

    /**
     * ServiceNotProvided
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Server does not provide this service."
     */
    int SERVICE_NOT_PROVIDED = 671;

    /**
     * ClientBoundToServer
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_PROTECT},
     * argument count 2,
     * Text: "Client '%client%' is restricted to use on server '%serverID%'."
     */
    int CLIENT_BOUND_TO_SERVER = 672;

    /**
     * LineTooLong
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Logfile line too long. Maximum length is %maxLineLen%. This length can be increased by setting the filesys.bufsize configurable."
     */
    int LINE_TOO_LONG = 673;

    /**
     * ExTORETYPE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] file(s) to retype."
     */
    int EX_TORETYPE = 674;

    /**
     * DepotNotSpec
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "%'SpecMap'% entries may only be added for a depot of type '%'spec'%'."
     */
    int DEPOT_NOT_SPEC = 675;

    /**
     * ReconcileBadName
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 1,
     * Text: "%file% - can't %'reconcile'% filename with illegal characters."
     */
    int RECONCILE_BAD_NAME = 676;

    /**
     * ReconcileNeedForce
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 1,
     * Text: "%file% - can't %'reconcile'% filename with wildcards [@#%%*]. Use -f to force %'reconcile'%."
     */
    int RECONCILE_NEED_FORCE = 677;

    /**
     * PopulateTargetExists
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 0,
     * Text: "Can't populate target path when files already exist."
     */
    int POPULATE_TARGET_EXISTS = 678;

    /**
     * PopulateDesc
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Populate %target%[ from %source%]."
     */
    int POPULATE_DESC = 679;

    /**
     * StreamNoFlow
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 1,
     * Text: "Stream '%stream%' type of %'virtual'% cannot have '%'toparent'%' or '%'fromparent'%' options set."
     */
    int STREAM_NO_FLOW = 680;

    /**
     * StreamIsVirtual
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Stream '%name%' is a virtual stream (command not allowed)."
     */
    int STREAM_IS_VIRTUAL = 681;

    /**
     * IntegBaselessMove
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile% - resolve move to %moveFile% before integrating from %fromFile%"
     */
    int INTEG_BASELESS_MOVE = 682;

    /**
     * ExTORECONCILE
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] file(s) to reconcile."
     */
    int EX_TORECONCILE = 683;

    /**
     * StatusSuccess
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%localfile% - reconcile to %action% %depotFile%%workRev%"
     */
    int STATUS_SUCCESS = 684;

    /**
     * ClassicSwitch
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Client '%client%' has a static view that will be overwritten; use -f to force switch."
     */
    int CLASSIC_SWITCH = 685;

    /**
     * TraitIsOpen
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 0,
     * Text: "Cannot set propagating traits on currently opened file(s)."
     */
    int TRAIT_IS_OPEN = 686;

    /**
     * OpenAttrRO
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 2,
     * Text: "%file% - can't %action% file with propagating attributes from an edge server"
     */
    int OPEN_ATTR_RO = 687;

    /**
     * ShelveNotChanged
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%depotRev% - unchanged, not shelved"
     */
    int SHELVE_NOT_CHANGED = 688;

    /**
     * EmbSpecChar
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Embedded special characters (*, %%, #, @) not allowed in '%path%'."
     */
    int EMB_SPEC_CHAR = 700;

    /**
     * DepotUnloadDup
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 1,
     * Text: "There is already an %'unload'% depot called '%depot%'."
     */
    int DEPOT_UNLOAD_DUP = 701;

    /**
     * DomainIsUnloaded
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONFIG},
     * argument count 2,
     * Text: "%domainType% %domain% has been unloaded, and must be reloaded to be used."
     */
    int DOMAIN_IS_UNLOADED = 702;

    /**
     * NotClientOrLabel
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONFIG},
     * argument count 2,
     * Text: "%domain% must be a %domainType%."
     */
    int NOT_CLIENT_OR_LABEL = 703;

    /**
     * NotUnloaded
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONFIG},
     * argument count 1,
     * Text: "%domain% does not require reloading."
     */
    int NOT_UNLOADED = 704;

    /**
     * AlreadyUnloaded
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONFIG},
     * argument count 1,
     * Text: "%domain% has already been unloaded."
     */
    int ALREADY_UNLOADED = 705;

    /**
     * CantChangeUnloadedOpt
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "The autoreload/noautoreload option may not be modified."
     */
    int CANT_CHANGE_UNLOADED_OPT = 706;

    /**
     * UnloadDepotMissing
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "No unload depot has been defined for this server."
     */
    int UNLOAD_DEPOT_MISSING = 707;

    /**
     * UnloadNotOwner
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Client, label, or task stream %domainName% is not owned by you."
     */
    int UNLOAD_NOT_OWNER = 708;

    /**
     * ReloadNotOwner
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Unloaded client, label, or task stream %domainName% is not owned by you."
     */
    int RELOAD_NOT_OWNER = 709;

    /**
     * IntegPreviewResolve
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 5,
     * Text: "must resolve %resolveType% from %fromFile%%fromRev%[ using base %baseFile%][%baseRev%]"
     */
    int INTEG_PREVIEW_RESOLVE = 710;

    /**
     * IntegPreviewResolveMove
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "must resolve move to %fromFile%[ using base %baseFile%]"
     */
    int INTEG_PREVIEW_RESOLVE_MOVE = 711;

    /**
     * NoSuchServerlog
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "No such logfile '%logfile%'."
     */
    int NO_SUCH_SERVERLOG = 714;

    /**
     * UnloadData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%object% %name% unloaded."
     */
    int UNLOAD_DATA = 715;

    /**
     * ReloadData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%object% %name% reloaded."
     */
    int RELOAD_DATA = 716;

    /**
     * NoUnloadedAutoLabel
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "An automatic label may not specify the autoreload option."
     */
    int NO_UNLOADED_AUTO_LABEL = 717;

    /**
     * GroupExists
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "No permission to modify existing group %group%."
     */
    int GROUP_EXISTS = 718;

    /**
     * ServerTypeMismatch
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 0,
     * Text: "Server type is not appropriate for specified server services."
     */
    int SERVER_TYPE_MISMATCH = 719;

    /**
     * ServiceUserLogin
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "Remote server refused request. Please verify that service user is correctly logged in to remote server, then retry."
     */
    int SERVICE_USER_LOGIN = 720;

    /**
     * IntegMovedNoFrom
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%depotFile% - can't %action% from %fromFile%%fromRev% (moved from %movedFrom%; provide a branch view that maps this file, or use -Di to disregard move/deletes)"
     */
    int INTEG_MOVED_NO_FROM = 721;

    /**
     * ReloadFirst
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Unload depot %depot% isn't empty of unload files; reload any unloaded clients or labels with '%'p4 reload'%' first. All labels with the '%'autoreload'%' option set must be deleted prior to deleting the unload depot. Remove all non-Perforce files from the depot prior to depot deletion."
     */
    int RELOAD_FIRST = 722;

    /**
     * AdminPasswordData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%user% - must reset password"
     */
    int ADMIN_PASSWORD_DATA = 723;

    /**
     * OpenOtherDepot
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%clientFile% - can't open %depotFile% (already opened as %depotFile2%)"
     */
    int OPEN_OTHER_DEPOT = 724;

    /**
     * ExINTEGMOVEDEL
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - move/delete(s)|Move/delete(s)] must be integrated along with matching move/add(s)."
     */
    int EX_INTEGMOVEDEL = 725;

    /**
     * PopulateTargetMixed
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 0,
     * Text: "Can't update target with mixed stream/non-stream paths."
     */
    int POPULATE_TARGET_MIXED = 726;

    /**
     * PopulateInvalidStream
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 1,
     * Text: "Can't update stream target with '%depotFile%'"
     */
    int POPULATE_INVALID_STREAM = 727;

    /**
     * PopulateMultipleStreams
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 0,
     * Text: "Can't update multiple streams with single command."
     */
    int POPULATE_MULTIPLE_STREAMS = 728;

    /**
     * StreamNoReparent
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "Cannot change Parent '%parent%' in %type% stream '%stream%'."
     */
    int STREAM_NO_REPARENT = 729;

    /**
     * VerifyDataProblem
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 7,
     * Text: "%depotFile%%depotRev% - %action% %change% (%type%) %digest% %status%"
     */
    int VERIFY_DATA_PROBLEM = 730;

    /**
     * PropertyData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%settingName% = %settingNalue%"
     */
    int PROPERTY_DATA = 731;

    /**
     * PropertyDataUser
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%settingName% = %settingNalue% (user %userName%)"
     */
    int PROPERTY_DATA_USER = 732;

    /**
     * PropertyDataGroup
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%settingName% = %settingNalue% (group %groupName%)"
     */
    int PROPERTY_DATA_GROUP = 733;

    /**
     * NoSuchProperty
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 4,
     * Text: "No such property '%settingName%' sequence %settingSequence% user %settingUser% group %settingGroup%."
     */
    int NO_SUCH_PROPERTY = 734;

    /**
     * ExPROPERTY
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%settingName% - no|No] such property."
     */
    int EX_PROPERTY = 735;

    /**
     * StreamNoConvert
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Cannot convert '%type%' stream to '%'task'%' stream."
     */
    int STREAM_NO_CONVERT = 736;

    /**
     * PurgeActiveTask
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 2,
     * Text: "Can't %action% active task stream files - '%depotFile%'"
     */
    int PURGE_ACTIVE_TASK = 737;

    /**
     * StreamConverted
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Stream '%name%' converted from '%'task'%' to '%type%'."
     */
    int STREAM_CONVERTED = 738;

    /**
     * StreamParentIsTask
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Parent stream '%name%' is a task stream, child streams not allowed."
     */
    int STREAM_PARENT_IS_TASK = 739;

    /**
     * StreamBadConvert
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Cannot convert '%'task'%' stream to '%type%' - parent from different depot."
     */
    int STREAM_BAD_CONVERT = 740;

    /**
     * NoRevisionOverwrite
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 2,
     * Text: "Revision %depotFile%#%depotRev% already exists! A submit operation attempted to overwrite this revision with a new file at the same revision. This should never happen, and therefore the server has aborted the submit. It is possible that the Perforce database files have been corrupted by a disk failure, system crash, or improper restore operation. Please contact Perforce technical support for assistance. Please do not perform any further operations on the server until the problem can be resolved. Please save all server logs, journals, and database tables for use in determining the necessary recovery operations."
     */
    int NO_REVISION_OVERWRITE = 741;

    /**
     * ShelvedHasWorking
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Cannot submit - files are open by client %client% at change %change%."
     */
    int SHELVED_HAS_WORKING = 742;

    /**
     * OpenedTaskSwitch
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Client '%client%' has files opened from a task stream; must %'revert'%, or %'revert -k'% before switching."
     */
    int OPENED_TASK_SWITCH = 743;

    /**
     * UnshelveNotTask
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%depotFile% - can't unshelve for %action%, task stream client required."
     */
    int UNSHELVE_NOT_TASK = 744;

    /**
     * ServerViewMap
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 0,
     * Text: "This type of view mapping may not be provided for this server."
     */
    int SERVER_VIEW_MAP = 745;

    /**
     * FiltersReplicaOnly
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 0,
     * Text: "Data Filters should be specified only for replica servers."
     */
    int FILTERS_REPLICA_ONLY = 746;

    /**
     * BadSequence
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Invalid sequence number '%seq%'."
     */
    int BAD_SEQUENCE = 747;

    /**
     * StreamIsUnloaded
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONFIG},
     * argument count 2,
     * Text: "Client %client% cannot be used with unloaded stream %stream%, switch to another stream or reload it."
     */
    int STREAM_IS_UNLOADED = 748;

    /**
     * IdEmbeddedNul
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Embedded null bytes not allowed - '%id%'."
     */
    int ID_EMBEDDED_NUL = 749;

    /**
     * UserCantChange
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 0,
     * Text: "User cannot be changed in a committed change."
     */
    int USER_CANT_CHANGE = 750;

    /**
     * DepotHasStreams
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Depot '%depot%' is the location of existing streams; cannot delete until they are removed."
     */
    int DEPOT_HAS_STREAMS = 751;

    /**
     * OpenTaskNotMapped
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%clientFile% - can't open %depotFile% (not mapped in client), must sync first."
     */
    int OPEN_TASK_NOT_MAPPED = 752;

    /**
     * StreamTargetExists
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%stream% - can't create stream where files already exist."
     */
    int STREAM_TARGET_EXISTS = 753;

    /**
     * RmtDeleteDomainFailed
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Commit server access failed while trying to delete a domain named %domainName%."
     */
    int RMT_DELETE_DOMAIN_FAILED = 754;

    /**
     * RemoteDomainExists
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "A domain named %domainName% already exists in this installation."
     */
    int REMOTE_DOMAIN_EXISTS = 755;

    /**
     * RemoteDomainMissing
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "There is no domain named %domainName% in this installation."
     */
    int REMOTE_DOMAIN_MISSING = 756;

    /**
     * RmtAddDomainFailed
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Commit server access failed while trying to add a domain named %domainName%."
     */
    int RMT_ADD_DOMAIN_FAILED = 757;

    /**
     * RmtExclusiveLockFailed
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "Commit server access failed while trying to get/release exclusive (+l) filetype."
     */
    int RMT_EXCLUSIVE_LOCK_FAILED = 758;

    /**
     * IntegMovedNoFromS
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%depotFile% - can't %action% from %fromFile%%fromRev% (moved from %movedFrom%; provide a branch/stream view that maps this file, or use 'p4 copy' to force)"
     */
    int INTEG_MOVED_NO_FROM_S = 759;

    /**
     * ReopenBadType
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%workRev% - can't change +l type with reopen; use revert -k and then edit -t to change type."
     */
    int REOPEN_BAD_TYPE = 760;

    /**
     * BoundToOtherServer
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_PROTECT},
     * argument count 4,
     * Text: "%objectType% '%objectName%' is restricted to use on server '%domainServerID%', not on server '%serverID%'."
     */
    int BOUND_TO_OTHER_SERVER = 761;

    /**
     * NotBoundToServer
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_PROTECT},
     * argument count 3,
     * Text: "%objectType% '%objectName%' is not restricted to use on server '%serverID%'."
     */
    int NOT_BOUND_TO_SERVER = 762;

    /**
     * MoveNoInteg
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - moved from %movedFile% but has no matching resolve record; must 'add -d' or 'move' to correct."
     */
    int MOVE_NO_INTEG = 763;

    /**
     * ResolveCharset
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "Charset resolve"
     */
    int RESOLVE_CHARSET = 764;

    /**
     * ResolveCharsetActionT
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "overwrite your open character set with their character set of %charset%"
     */
    int RESOLVE_CHARSET_ACTION_T = 765;

    /**
     * ResolveCharsetActionY
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "leave your character set of %charset% unchanged"
     */
    int RESOLVE_CHARSET_ACTION_Y = 766;

    /**
     * ReopenCharSet
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile%%workRev% - reopened; charset %charset%"
     */
    int REOPEN_CHAR_SET = 767;

    /**
     * IntegPreviewResolved
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 3,
     * Text: "%how% %fromFile%%fromRev%"
     */
    int INTEG_PREVIEW_RESOLVED = 768;

    /**
     * TooManyCommitServers
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "At most one commit-server may be defined. Server %serverid% is already specified to be a commit-server."
     */
    int TOO_MANY_COMMIT_SERVERS = 769;

    /**
     * OpenExclOrphaned
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile% - %user%@%client% *orphaned*"
     */
    int OPEN_EXCL_ORPHANED = 770;

    /**
     * OpenExclLocked
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%depotFile% - %user%@%client% *exclusive*"
     */
    int OPEN_EXCL_LOCKED = 771;

    /**
     * OpenExclOther
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%depotFile% - %user%@%client% %server%"
     */
    int OPEN_EXCL_OTHER = 772;

    /**
     * BindingNotAllowed
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 3,
     * Text: "%objectType% '%objectName%' should not be restricted to use on server '%serverID%'."
     */
    int BINDING_NOT_ALLOWED = 773;

    /**
     * PropertyAllData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%settingName% = %settingNalue% (%appliesToType%[ %appliesTo%]) [%sequence%]"
     */
    int PROPERTY_ALL_DATA = 774;

    /**
     * MustForceUnloadDepot
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "The Commit Server cannot tell whether unload depot %depot% may still be in use by unloaded clients or labels on one or more Edge Servers. First, reload any unloaded clients or labels at each Edge Server with '%'p4 reload'%'. Next, all labels on each Edge Server with the '%'autoreload'%' option set must be deleted prior to deleting the unload depot. Next, remove all non-Perforce files from the depot prior to depot deletion. Finally, specify -f to bypass this check and force the unload depot deletion."
     */
    int MUST_FORCE_UNLOAD_DEPOT = 775;

    /**
     * UnloadNotPossible
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%domainType% %domainName% has file(s) exclusively or globally opened or has promoted shelves, and may not be unloaded. Revert these opened files and delete the promoted shelves, then retry the unload."
     */
    int UNLOAD_NOT_POSSIBLE = 776;

    /**
     * OpenXOpenedFailed
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't %action% exclusive file already opened"
     */
    int OPEN_XOPENED_FAILED = 777;

    /**
     * CopyMoveMapFrom
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "can't open as move/add because %movedFrom% is not mapped correctly."
     */
    int COPY_MOVE_MAP_FROM = 778;

    /**
     * CopyMoveNoFrom
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "can't open as move/add because %movedFrom% is not being opened for delete."
     */
    int COPY_MOVE_NO_FROM = 779;

    /**
     * CopyMoveExTo
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "can't open as move/add because a file already exists in this location."
     */
    int COPY_MOVE_EX_TO = 780;

    /**
     * CopyMapSummary
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "Some files couldn't be opened for move.  Try expanding the mapping?"
     */
    int COPY_MAP_SUMMARY = 781;

    /**
     * CopyChangeSummary
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Some files couldn't be opened for move.[  Try copying from @%change% instead?]"
     */
    int COPY_CHANGE_SUMMARY = 782;

    /**
     * RmtAddChangeFailed
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Commit server access failed while trying to add change %change%."
     */
    int RMT_ADD_CHANGE_FAILED = 783;

    /**
     * RmtDeleteChangeFailed
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Commit server access failed while trying to delete change %change%."
     */
    int RMT_DELETE_CHANGE_FAILED = 784;

    /**
     * RemoteChangeExists
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Change %change% already exists in this installation."
     */
    int REMOTE_CHANGE_EXISTS = 785;

    /**
     * RemoteChangeMissing
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "There is no change %change% in this installation."
     */
    int REMOTE_CHANGE_MISSING = 786;

    /**
     * ChangeNotShelved
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "%change% is not currently shelved."
     */
    int CHANGE_NOT_SHELVED = 787;

    /**
     * FilesSummaryHuman
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%path% %fileCount% files %fileSize% [%blockCount% blocks]"
     */
    int FILES_SUMMARY_HUMAN = 788;

    /**
     * FilesDiskUsageHuman
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 4,
     * Text: "%depotFile%%depotRev% %fileSize% [%blockCount% blocks]"
     */
    int FILES_DISK_USAGE_HUMAN = 789;

    /**
     * ExARCHIVES
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] such cached archive(s)."
     */
    int EX_ARCHIVES = 790;

    /**
     * CachePurgeFile
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "Purging content of %lbrFile% %lbrRev% size %archiveSize%."
     */
    int CACHE_PURGE_FILE = 791;

    /**
     * AddHaveVsRev
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "File %depotFile% isn't in revisions table. The file may have been obliterated by an administrator. Make a backup copy of your local file before proceeding. Then use 'p4 sync' to synchronize your client state with the server. Then copy the desired file back into your workspace location. Then retry the add."
     */
    int ADD_HAVE_VS_REV = 792;

    /**
     * TriggerNoDepotFile
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "Trigger depot file '%file%' not found, purged or wrong type for trigger '%trigger%'."
     */
    int TRIGGER_NO_DEPOT_FILE = 793;

    /**
     * TriggerNoArchiveType
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Archive trigger may not use trigger depot files."
     */
    int TRIGGER_NO_ARCHIVE_TYPE = 794;

    /**
     * ParallelOptions
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Usage: threads=N,batch=N,batchsize=N,min=N,minsize=N"
     */
    int PARALLEL_OPTIONS = 795;

    /**
     * ParallelNotEnabled
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Parallel file transfer must be enabled using %'net.parallel.max'%"
     */
    int PARALLEL_NOT_ENABLED = 796;

    /**
     * UnshelveFromRemote
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't unshelve from remote server (already opened for %badAction%)"
     */
    int UNSHELVE_FROM_REMOTE = 797;

    /**
     * ResolveMustIgnore
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%clientFile% - their revision is unavailable; %'resolve -ay'% (ignore) or revert?"
     */
    int RESOLVE_MUST_IGNORE = 798;

    /**
     * OpenWarnChangeMap
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%depotFile% - warning: cannot submit file that is restricted [to %change% ]by client's ChangeView mapping"
     */
    int OPEN_WARN_CHANGE_MAP = 799;

    /**
     * ReloadSuspicious
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Input file contains an incorrect record for %tableName%."
     */
    int RELOAD_SUSPICIOUS = 800;

    /**
     * ServersJnlAckData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 9,
     * Text: "%serverID% '%lastUpdate%' %serverType% %persistedJnl%/%persistedPos% %appliedJnl%/%appliedPos% %jaFlags% %isAlive%"
     */
    int SERVERS_JNL_ACK_DATA = 801;

    /**
     * CantChangeUserAuth
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "User's authentication method can't be changed; use -f to force switch."
     */
    int CANT_CHANGE_USER_AUTH = 802;

    /**
     * BadPortNumber
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 0,
     * Text: "Port numbers must be in the range 1 to 65535."
     */
    int BAD_PORT_NUMBER = 803;

    /**
     * LdapData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%name% %host%:%port% %type% (%status%)"
     */
    int LDAP_DATA = 804;

    /**
     * LdapConfBadOwner
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "LDAP configuration directory or files not owned by Perforce process effective user."
     */
    int LDAP_CONF_BAD_OWNER = 805;

    /**
     * LdapConfBadPerms
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "LDAP configuration directory must have 700 permissions."
     */
    int LDAP_CONF_BAD_PERMS = 806;

    /**
     * LdapSave
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "LDAP configuration %name% saved."
     */
    int LDAP_SAVE = 807;

    /**
     * LdapNoChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "LDAP configuration %name% not changed."
     */
    int LDAP_NO_CHANGE = 808;

    /**
     * LdapDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "LDAP configuration %name% deleted."
     */
    int LDAP_DELETE = 809;

    /**
     * NoSuchLdap
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "LDAP configuration '%name%' doesn't exist."
     */
    int NO_SUCH_LDAP = 810;

    /**
     * LdapRequiredField0
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 0,
     * Text: "Bind type 'simple' requires SimplePattern to be set."
     */
    int LDAP_REQUIRED_FIELD_0 = 811;

    /**
     * LdapRequiredField1
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 0,
     * Text: "Bind type 'search' requires SearchBaseDN to be set."
     */
    int LDAP_REQUIRED_FIELD_1 = 812;

    /**
     * LdapRequiredField2
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 0,
     * Text: "Bind type 'search' requires SearchFilter to be set."
     */
    int LDAP_REQUIRED_FIELD_2 = 813;

    /**
     * LdapRequiredField3
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 0,
     * Text: "GroupSearchFilter requires SearchBaseDN or GroupBaseDN to be set."
     */
    int LDAP_REQUIRED_FIELD_3 = 814;

    /**
     * OpenHasResolve
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 2,
     * Text: "%clientFile% - can't %action% file with pending integrations."
     */
    int OPEN_HAS_RESOLVE = 815;

    /**
     * StreamVsTemplate
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Stream and template table out of sync for stream %stream%!"
     */
    int STREAM_VS_TEMPLATE = 816;

    /**
     * NoSharedRevision
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Cannot import %depotFile% because there is no existing revision."
     */
    int NO_SHARED_REVISION = 817;

    /**
     * SharedActionMismatch
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 3,
     * Text: "Conflict: %change% %depotFile%%depotRev% (%action%)."
     */
    int SHARED_ACTION_MISMATCH = 818;

    /**
     * SharedDigestMismatch
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 6,
     * Text: "Cannot import %importFile%%importRev% (%importDigest%) over %depotFile%%depotRev% (%digest%)"
     */
    int SHARED_DIGEST_MISMATCH = 819;

    /**
     * UnshelveBadChangeView
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 2,
     * Text: "%depotFile% - can't unshelve from revision at change %change% (restricted by client's ChangeView)"
     */
    int UNSHELVE_BAD_CHANGE_VIEW = 820;

    /**
     * ClientNoSwitch
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Client '%client%' already exists; use a new client or -s without -o to switch to different stream."
     */
    int CLIENT_NO_SWITCH = 821;

    /**
     * RevisionAlreadyPresent
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Cannot import %depotFile% because there is already an existing revision."
     */
    int REVISION_ALREADY_PRESENT = 822;

    /**
     * UnrecognizedRevision
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Cannot import %depotFile% because its history is unrecognizable."
     */
    int UNRECOGNIZED_REVISION = 823;

    /**
     * NoLazySource
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Cannot import %depotFile%#%depotRev% because the source revision cannot be found."
     */
    int NO_LAZY_SOURCE = 824;

    /**
     * OpenWarnReaddMoved
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "opened for %action% as new file; use '%'move'%' to recover moved files."
     */
    int OPEN_WARN_READD_MOVED = 825;

    /**
     * InvalidZipFormat
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Invalid zipfile format: %details%."
     */
    int INVALID_ZIP_FORMAT = 826;

    /**
     * UnsubmitNotHead
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 3,
     * Text: "Cannot unsubmit %depotFile%%depotRev% because the head revision is %headRev%"
     */
    int UNSUBMIT_NOT_HEAD = 827;

    /**
     * UnsubmitOpened
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 3,
     * Text: "Cannot unsubmit %depotFile%%depotRev% because it is currently opened by %client%"
     */
    int UNSUBMIT_OPENED = 828;

    /**
     * UnsubmitNotSubmitted
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Cannot unsubmit change %change% because it is not a submitted change."
     */
    int UNSUBMIT_NOT_SUBMITTED = 829;

    /**
     * UnsubmitWrongUser
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Cannot unsubmit change %change% because it was submitted by another user."
     */
    int UNSUBMIT_WRONG_USER = 830;

    /**
     * UnsubmitWrongClient
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Cannot unsubmit change %change% because it was submitted by another client."
     */
    int UNSUBMIT_WRONG_CLIENT = 831;

    /**
     * UnsubmitIntegrated
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 3,
     * Text: "Cannot unsubmit %change% because %depotFile%%depotRev% has been integrated elsewhere."
     */
    int UNSUBMIT_INTEGRATED = 832;

    /**
     * UnsubmitNoInteg
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "Integration record mismatch."
     */
    int UNSUBMIT_NO_INTEG = 833;

    /**
     * ZipIntegMismatch
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "Integration record mismatch."
     */
    int ZIP_INTEG_MISMATCH = 834;

    /**
     * ZipBranchDidntMap
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Specified branch map is missing an entry for %depotFile%."
     */
    int ZIP_BRANCH_DIDNT_MAP = 835;

    /**
     * RemoteSave
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Remote %remoteName% saved."
     */
    int REMOTE_SAVE = 836;

    /**
     * RemoteNoChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Remote %remoteName% not changed."
     */
    int REMOTE_NO_CHANGE = 837;

    /**
     * RemoteDelete
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Remote %remoteName% deleted."
     */
    int REMOTE_DELETE = 838;

    /**
     * NoSuchRemote
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_UNKNOWN},
     * argument count 1,
     * Text: "Remote '%remote%' doesn't exist."
     */
    int NO_SUCH_REMOTE = 839;

    /**
     * RemotesData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "%remoteID% %address% '%description%'"
     */
    int REMOTES_DATA = 840;

    /**
     * ImportedChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Change %change% imported as change %finalChange%."
     */
    int IMPORTED_CHANGE = 841;

    /**
     * ImportedFile
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "File %depotFile%%depotRev% imported as %targetRev%."
     */
    int IMPORTED_FILE = 842;

    /**
     * ImportedIntegration
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Integration imported from %fromFile% to %toFile%."
     */
    int IMPORTED_INTEGRATION = 843;

    /**
     * ImportSkippedChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Change %change% was already present in the target repository."
     */
    int IMPORT_SKIPPED_CHANGE = 844;

    /**
     * ImportSkippedFile
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "File %depotFile%%depotRev% was already present in the target repository."
     */
    int IMPORT_SKIPPED_FILE = 845;

    /**
     * ImportNoPermission
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 1,
     * Text: "Cannot import '%depotFile%' - protected namespace - access denied"
     */
    int IMPORT_NO_PERMISSION = 846;

    /**
     * ImportNoDepot
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 1,
     * Text: "Cannot import '%depotFile%' because it is in an unknown depot."
     */
    int IMPORT_NO_DEPOT = 847;

    /**
     * ImportDepotReadOnly
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 1,
     * Text: "Cannot import '%depotFile%' because this is a read-only depot."
     */
    int IMPORT_DEPOT_READ_ONLY = 848;

    /**
     * ExOPENNOTEDITADD
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - file(s)|File(s)] not opened for edit or add."
     */
    int EX_OPENNOTEDITADD = 849;

    /**
     * UnsubmitNoTraits
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "Cannot unsubmit %depotFile%%depotRev% because it has associated attributes."
     */
    int UNSUBMIT_NO_TRAITS = 850;

    /**
     * ImportSkippedInteg
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Integration from %fromFile% to %toFile% was already present in the target repository."
     */
    int IMPORT_SKIPPED_INTEG = 851;

    /**
     * StatusOpened
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%localfile% - submit change %change% to %action% %depotFile%%workRev%"
     */
    int STATUS_OPENED = 852;

    /**
     * NoSharedHistory
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "Cannot import %depotFile%%depotRev% because it is not common to both file histories."
     */
    int NO_SHARED_HISTORY = 853;

    /**
     * UnsubmitArchived
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 3,
     * Text: "Cannot unsubmit %depotFile%%depotRev% - %action% has occurred."
     */
    int UNSUBMIT_ARCHIVED = 854;

    /**
     * UnsubmittedChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Change %change% unsubmitted and shelved."
     */
    int UNSUBMITTED_CHANGE = 855;

    /**
     * ParSubOptions
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Usage: threads=N,batch=N,min=N"
     */
    int PAR_SUB_OPTIONS = 856;

    /**
     * ImportDanglingInteg
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Integration from %fromFile% to %toFile% ignored due to missing revision."
     */
    int IMPORT_DANGLING_INTEG = 857;

    /**
     * UnzipCouldntLock
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Cannot import revisions. Not all files could be %action%."
     */
    int UNZIP_COULDNT_LOCK = 858;

    /**
     * UnzipIsLocked
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 3,
     * Text: "%depotFile% - locked by %user%@%client%"
     */
    int UNZIP_IS_LOCKED = 859;

    /**
     * OpenCantMissing
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile% - can't %action% missing file"
     */
    int OPEN_CANT_MISSING = 860;

    /**
     * GroupLdapIncomplete
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 0,
     * Text: "All three LDAP fields must be set for LDAP group synchronization."
     */
    int GROUP_LDAP_INCOMPLETE = 861;

    /**
     * GroupLdapNoOwner
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 0,
     * Text: "At least one group owner is required for LDAP group synchronization."
     */
    int GROUP_LDAP_NO_OWNER = 862;

    /**
     * UnsubmitTaskStream
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "Cannot unsubmit %depotFile%%depotRev% - it is in an active task stream."
     */
    int UNSUBMIT_TASK_STREAM = 863;

    /**
     * ResubmitNoFiles
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Cannot resubmit shelved %change% because no shelved files were found."
     */
    int RESUBMIT_NO_FILES = 864;

    /**
     * ResubmitStreamClassic
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Cannot resubmit these changes because some affect stream depots while others affect classic depots."
     */
    int RESUBMIT_STREAM_CLASSIC = 865;

    /**
     * ResubmitMultiStream
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Cannot resubmit change %change% because it affects multiple stream depots."
     */
    int RESUBMIT_MULTI_STREAM = 866;

    /**
     * AdminSetLdapUserData
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%user% - now authenticates against LDAP"
     */
    int ADMIN_SET_LDAP_USER_DATA = 867;

    /**
     * UnsubmittedRenamed
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Change %change% renamed as %origChange%, unsubmitted and shelved."
     */
    int UNSUBMITTED_RENAMED = 868;

    /**
     * UnzipChangePresent
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "An attempt was made to import %change% into this server, but a change record for that change is already present!"
     */
    int UNZIP_CHANGE_PRESENT = 869;

    /**
     * UnzipRevisionPresent
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 2,
     * Text: "An attempt was made to import %depotFile%%depotRev% into this server, but a revision record for that revision is already present!"
     */
    int UNZIP_REVISION_PRESENT = 870;

    /**
     * UnzipIntegrationPresent
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 4,
     * Text: "An attempt was made to import integration %toFile% %how% %fromFile% %fromRevRange% into this server, but an integration record for that integration is already present!"
     */
    int UNZIP_INTEGRATION_PRESENT = 871;

    /**
     * StreamNotRelative
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 0,
     * Text: "Must specify a full stream path if not currently using a stream client."
     */
    int STREAM_NOT_RELATIVE = 872;

    /**
     * UnzipArchiveUnknown
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 4,
     * Text: "Partner server sent an unexpected archive! For index %index%, the supplied archive was %lbrFile% %lbrRev%, but our archive decision was %decision%."
     */
    int UNZIP_ARCHIVE_UNKNOWN = 873;

    /**
     * UnsubmitNoChanges
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "No changes were found matching %filespec% that could be unsubmitted."
     */
    int UNSUBMIT_NO_CHANGES = 874;

    /**
     * AdminSetLdapUserNoSuper
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "At least one super user with AuthMethod of 'perforce' must exist to perform this operation."
     */
    int ADMIN_SET_LDAP_USER_NO_SUPER = 875;

    /**
     * RmtGlobalLockFailed
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "Commit server access failed while trying to get/release global lock on file."
     */
    int RMT_GLOBAL_LOCK_FAILED = 876;

    /**
     * UnzipIsTaskStream
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 3,
     * Text: "File %depotFile%%depotRev% cannot be imported. This version of the server does not support copying changes from a remote server into the task stream %streamName%. Exclude the task stream's files from the remote map, or map them to an alternate destination, or delete the task stream from the destination server, and retry the operation."
     */
    int UNZIP_IS_TASK_STREAM = 877;

    /**
     * TangentBadSource
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 2,
     * Text: "Can't create a tangent of %depotFile% because it is in a %badPlace%."
     */
    int TANGENT_BAD_SOURCE = 878;

    /**
     * TangentBlockedDepot
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "A %depotType% depot called '%'tangent'%' already exists; must create a new %'tangent'% depot."
     */
    int TANGENT_BLOCKED_DEPOT = 879;

    /**
     * TangentBranchedFile
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%depotRev% - branched to tangent"
     */
    int TANGENT_BRANCHED_FILE = 880;

    /**
     * TangentMovedFile
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%depotFile%%depotRev% - relocated to tangent"
     */
    int TANGENT_MOVED_FILE = 881;

    /**
     * UnzipNoSuchArchive
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Input zip file does not contain an archive entry for %archiveEntry%."
     */
    int UNZIP_NO_SUCH_ARCHIVE = 882;

    /**
     * DepotDepthDiffers
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 1,
     * Text: "Cannot change stream depth '%depotDepth%' when streams or depot archives already exist."
     */
    int DEPOT_DEPTH_DIFFERS = 883;

    /**
     * UnsubmitEmptyChange
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Cannot unsubmit change %change% because it is an empty change."
     */
    int UNSUBMIT_EMPTY_CHANGE = 884;

    /**
     * DepotTypeDup
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 2,
     * Text: "There is already a %depotType% depot called '%depot%'."
     */
    int DEPOT_TYPE_DUP = 885;

    /**
     * NoStorageDir
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONFIG},
     * argument count 1,
     * Text: "'%type%' client type has not been configured for this server.\nStorage location '%'client.readonly.dir'%' needs to be set by the administrator."
     */
    int NO_STORAGE_DIR = 886;

    /**
     * ChangeIdentityAlready
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 3,
     * Text: "%change% may not be given identity %identity% because that identity has already been used by %existingChange%."
     */
    int CHANGE_IDENTITY_ALREADY = 887;

    /**
     * BadChangeMap
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Could not translate '%change%' into a changelist number. Change maps can only use changelist numbers or automatic labels. Please check your client or stream mappings."
     */
    int BAD_CHANGE_MAP = 888;

    /**
     * LabelNotAutomatic
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Label '%name%' isn't an automatic label. The Revision field is empty."
     */
    int LABEL_NOT_AUTOMATIC = 889;

    /**
     * LabelRevNotChange
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "The Revision field in label '%name%' isn't set to a changelist or date."
     */
    int LABEL_REV_NOT_CHANGE = 890;

    /**
     * ImportWouldAddChange
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Change %change% would be imported to the target repository."
     */
    int IMPORT_WOULD_ADD_CHANGE = 891;

    /**
     * ReservedClientName
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Client may not be named '%clientName%'; that is a reserved name."
     */
    int RESERVED_CLIENT_NAME = 892;

    /**
     * StreamDepthDiffers
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 2,
     * Text: "Stream %stream% name does not reflect depot depth-field '%depotDepth%'."
     */
    int STREAM_DEPTH_DIFFERS = 893;

    /**
     * CannotChangeStorageType
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Client storage type cannot be changed after client is created."
     */
    int CANNOT_CHANGE_STORAGE_TYPE = 894;

    /**
     * ServerLocksOrder
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 4,
     * Text: "Server locking failure: %objectType% %objectName% %lockOrder% locked after %currentLockOrder%!"
     */
    int SERVER_LOCKS_ORDER = 895;

    /**
     * DepotStreamDepthReq
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 1,
     * Text: "Depot '%depot%' of type stream requires StreamDepth field between 1-10."
     */
    int DEPOT_STREAM_DEPTH_REQ = 896;

    /**
     * DepotNotEmptyNoChange
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 2,
     * Text: "Cannot update depot %depot% type while it contains %objects%."
     */
    int DEPOT_NOT_EMPTY_NO_CHANGE = 897;

    /**
     * BucketSkipResolving
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "Not archiving %depotFile%%depotRev%: content needed by a client with a pending resolve to %clientFile%."
     */
    int BUCKET_SKIP_RESOLVING = 898;

    /**
     * BucketSkipShelving
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 3,
     * Text: "Not archiving %depotFile%%depotRev%: content needed by a shelf with a pending resolve to %shelfFile%."
     */
    int BUCKET_SKIP_SHELVING = 899;

    /**
     * ParThreadsTooMany
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "Number of threads (%threads%) exceeds net.parallel.max (%maxthreads%)."
     */
    int PAR_THREADS_TOO_MANY = 900;

    /**
     * ExUNLOADED
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - no|No] such unloaded client(s), label(s), or task stream(s)."
     */
    int EX_UNLOADED = 901;

    /**
     * DevMsg
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%text%"
     */
    int DEV_MSG = 902;

    /**
     * DevErr
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Internal error: %text%"
     */
    int DEV_ERR = 903;

    /**
     * StreamOpened
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "Stream %stream%[@%haveChange%] - opened on this client"
     */
    int STREAM_OPENED = 904;

    /**
     * StreamIsOpen
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Stream %stream% is already open on this client."
     */
    int STREAM_IS_OPEN = 905;

    /**
     * ExSTREAMOPEN
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_EMPTY},
     * argument count 1,
     * Text: "[%argc% - stream|Stream] not opened on this client."
     */
    int EX_STREAMOPEN = 906;

    /**
     * StreamReverted
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Stream %stream% reverted."
     */
    int STREAM_REVERTED = 907;

    /**
     * StreamResolve
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%localStream% %field% - resolving %fromStream%@%fromChange%[ using base @%baseChange%]"
     */
    int STREAM_RESOLVE = 908;

    /**
     * StreamResolved
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 5,
     * Text: "%localStream% %field% - %how% %fromStream%@%fromChange%"
     */
    int STREAM_RESOLVED = 909;

    /**
     * StreamResolveField
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%field% resolve"
     */
    int STREAM_RESOLVE_FIELD = 910;

    /**
     * StreamResolveAction
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%text%"
     */
    int STREAM_RESOLVE_ACTION = 911;

    /**
     * LockAlreadyCommit
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 4,
     * Text: "%depotFile% - already locked on Commit Server by %user%@%client% at change %change%"
     */
    int LOCK_ALREADY_COMMIT = 912;

    /**
     * StreamShelveMismatch
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 2,
     * Text: "Shelved stream %shelvedStream% does not match client stream [%clientStream%|(none)]."
     */
    int STREAM_SHELVE_MISMATCH = 913;

    /**
     * StreamNotOpen
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Client %client% does not have an open stream."
     */
    int STREAM_NOT_OPEN = 914;

    /**
     * StreamSwitchOpen
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Can't switch to %stream% while current stream spec has pending changes.  Use 'p4 stream revert' to discard."
     */
    int STREAM_SWITCH_OPEN = 915;

    /**
     * StreamMustResolve
     * Severity {@link MessageSeverityCode#E_WARN}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Stream %stream% is out of date; run 'p4 stream resolve'."
     */
    int STREAM_MUST_RESOLVE = 916;

    /**
     * StreamShelved
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Stream %stream% shelved."
     */
    int STREAM_SHELVED = 917;

    /**
     * StreamUnshelved
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "Stream %stream% unshelved."
     */
    int STREAM_UNSHELVED = 918;

    /**
     * StreamOpenBadType
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 1,
     * Text: "Not permitted to open stream spec with type '%type%'."
     */
    int STREAM_OPEN_BAD_TYPE = 919;

    /**
     * CounterNoTAS
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "New value for %counterName% not set. Current value is %counterValue%."
     */
    int COUNTER_NO_TA_S = 920;

    /**
     * WildAddFilename
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "The file named '%filename%' contains wildcards [@#%*]."
     */
    int WILD_ADD_FILENAME = 921;

    /**
     * JoinMax1TooSmall
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 1,
     * Text: "Command exceeded map.joinmax1 size (%joinmax1% bytes).  This length can be increased by setting the map.joinmax1 configurable."
     */
    int JOIN_MAX_1TOO_SMALL = 922;

    /**
     * RevChangedDuringPush
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "Conflict: A concurrent modification to %depotFile%%depotRev% occurred during this push/fetch/unzip operation, causing the import step to be halted."
     */
    int REV_CHANGED_DURING_PUSH = 923;

    /**
     * EmbEllipse
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Embedded wildcards (...) not allowed in '%path%'."
     */
    int EMB_ELLIPSE = 924;

    /**
     * OpenReadOnlyCMap
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%clientFile% - can't %action% file that is restricted by client's ChangeView mapping"
     */
    int OPEN_READ_ONLY_CMAP = 925;

    /**
     * IntegIntoReadOnlyAndMap
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%clientFile% - can't %action% into file that is additionally mapped in client's View"
     */
    int INTEG_INTO_READ_ONLY_AND_MAP = 926;

    /**
     * IntegIntoReadOnlyCMap
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%clientFile% - can't %action% into file that is restricted by client's ChangeView mapping"
     */
    int INTEG_INTO_READ_ONLY_CMAP = 927;

    /**
     * UnshelveBadAndmap
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 2,
     * Text: "%clientFile% - can't unshelve from revision at change %change% (additionally mapped in client's View)"
     */
    int UNSHELVE_BAD_ANDMAP = 928;

    /**
     * OpenWarnAndmap
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 2,
     * Text: "%clientFile% - warning: cannot submit file that is additionally mapped in client's View"
     */
    int OPEN_WARN_ANDMAP = 929;

    /**
     * OpenReadOnlyAndMap
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 2,
     * Text: "%clientFile% - can't %action% file that is additionally mapped in client's View"
     */
    int OPEN_READ_ONLY_AND_MAP = 930;

    /**
     * ServerConfigUsage
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 0,
     * Text: "Invalid DistributedConfig syntax: must use 'var=value'"
     */
    int SERVER_CONFIG_USAGE = 931;

    /**
     * ServerConfigInvalidVar
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 1,
     * Text: "Configuration variable '%name%' cannot be set from here."
     */
    int SERVER_CONFIG_INVALID_VAR = 932;

    /**
     * ServerConfigRO
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 2,
     * Text: "Configuration variable '%name%' must be set to '%value%'."
     */
    int SERVER_CONFIG_RO = 933;

    /**
     * ServerCantConfig
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 0,
     * Text: "%'DistributedConfig'% can only be set with %'-c'% option."
     */
    int SERVER_CANT_CONFIG = 934;

    /**
     * ServerSvcInvalid
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_CONTEXT},
     * argument count 2,
     * Text: "Configuration for '%services%' cannot be set on server that uses '%existingSvc%' Services."
     */
    int SERVER_SVC_INVALID = 935;

    /**
     * UnknownReadonlyDir
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 1,
     * Text: "Client %clientName% cannot be accessed, because clients of type %'readonly'% are unavailable if the %'client.readonly.dir'% configuration variable is invalid or unset."
     */
    int UNKNOWN_READONLY_DIR = 936;

    /**
     * ShelveNotSubmittable
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 1,
     * Text: "Shelved file %depotFile% has no submitted revisions. Perhaps the file was obliterated after the shelf was created, or perhaps the shelf was pushed from another server where the underlying file exists. You may unshelve the file and resolve it to specify that the new file should be added, or you may remove the file from the shelf, or you may submit the underlying revision first, but you may not submit this shelf as-is."
     */
    int SHELVE_NOT_SUBMITTABLE = 937;

    /**
     * NoSplitMoves
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NOTYET},
     * argument count 4,
     * Text: "Change %change% performs a %action% on %file%%rev%, but the parameters of this fetch, push, or zip command include only part of the full action. Specify a wider view to include both the source and target of the change, or specify a narrower view to exclude both the source and target of the change."
     */
    int NO_SPLIT_MOVES = 938;

    /**
     * ProtectsBadPerm
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Can't add '%perm%' entry to sub-protections table."
     */
    int PROTECTS_BAD_PERM = 939;

    /**
     * ProtectsPathOutOfScope
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "All paths in sub-protections table must be under path '%path%'."
     */
    int PROTECTS_PATH_OUT_OF_SCOPE = 940;

    /**
     * ProtectsOwnerEnds
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Paths in 'owner' entries must end with '/...'."
     */
    int PROTECTS_OWNER_ENDS = 941;

    /**
     * ProtectsOwnerWildcards
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Can't add 'owner' entry with embedded wildcards in path."
     */
    int PROTECTS_OWNER_WILDCARDS = 942;

    /**
     * ProtectsOwnerPath
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "No 'owner' entry matching path '%path%'."
     */
    int PROTECTS_OWNER_PATH = 943;

    /**
     * ProtectsDuplicateOwner
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Multiple 'owner' entries matching path '%path%' found."
     */
    int PROTECTS_DUPLICATE_OWNER = 944;

    /**
     * CallerMustForward
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 0,
     * Text: "Client requires forwarding."
     */
    int CALLER_MUST_FORWARD = 945;

    /**
     * CantForwardDelete
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Delete operation cancelled, main server is not reachable at the moment, try again later!"
     */
    int CANT_FORWARD_DELETE = 946;

    /**
     * LogFilenameInvalid
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Log filename is invalid."
     */
    int LOG_FILENAME_INVALID = 947;

    /**
     * LogFormatInvalid
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Log format is invalid."
     */
    int LOG_FORMAT_INVALID = 948;

    /**
     * LogNumericInvalid
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Log %property% must be numeric."
     */
    int LOG_NUMERIC_INVALID = 949;

    /**
     * LogEventsUnmatched
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Log captures no events."
     */
    int LOG_EVENTS_UNMATCHED = 950;

    /**
     * ProtectNoOwner
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "No 'owner' entry matching provided path."
     */
    int PROTECT_NO_OWNER = 951;

    /**
     * MoveReadOnlySrc
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%depotFile% - can't move from a spec or remote depot"
     */
    int MOVE_READ_ONLY_SRC = 952;

    /**
     * MoveCantForce
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_NONE},
     * argument count 1,
     * Text: "%clientFile% - is synced; can't use -r with existing target"
     */
    int MOVE_CANT_FORCE = 953;

    /**
     * ProtectsOwnerTooWide
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Paths in 'owner' entries must be more specific than '//...'."
     */
    int PROTECTS_OWNER_TOO_WIDE = 954;

    /**
     * ProtectsOwnerUnmap
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 0,
     * Text: "Can't add exclusionary 'owner' entries."
     */
    int PROTECTS_OWNER_UNMAP = 955;

    /**
     * LicensedRepos
     * Severity {@link MessageSeverityCode#E_INFO}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ADMIN},
     * argument count 2,
     * Text: "License count: %count% repos used of %max% licensed.\n"
     */
    int LICENSED_REPOS = 956;

    /**
     * StreamNotGraph
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Stream client cannot be of type '%'graph'%'."
     */
    int STREAM_NOT_GRAPH = 957;

    /**
     * CantChangeTypeOpened
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Can't change client type when files are open!"
     */
    int CANT_CHANGE_TYPE_OPENED = 958;

    /**
     * PurgeUnloadedTask
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_ILLEGAL},
     * argument count 2,
     * Text: "Can't %action% unloaded task stream files - '%depotFile%'"
     */
    int PURGE_UNLOADED_TASK = 959;

    /**
     * TriggerDuplicateType
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 1,
     * Text: "Only one trigger of type %type% allowed."
     */
    int TRIGGER_DUPLICATE_TYPE = 960;

    /**
     * TriggerIncomplete2FA
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "Incomplete set of second factor authentication triggers defined."
     */
    int TRIGGER_INCOMPLETE_2F_A = 961;

    /**
     * RevMissing
     * Severity {@link MessageSeverityCode#E_FATAL}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_FAULT},
     * argument count 2,
     * Text: "Revision %depotFile%%depotRev% is missing from the metadata! (Perhaps it was obliterated?)"
     */
    int REV_MISSING = 962;

    /**
     * CantChangeOwnDetails
     * Severity {@link MessageSeverityCode#E_FAILED}
     * Subsystem code {@link MessageSubsystemCode#ES_DM}
     * Generic code {@link MessageGenericCode#EV_USAGE},
     * argument count 0,
     * Text: "User's full name and email address can't be changed; use -f to force update."
     */
    int CANT_CHANGE_OWN_DETAILS = 963;
}
