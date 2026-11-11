# Gender Identity & Sexual Orientation Options - Phase 3

## Overview
These are the accepted values for `gender_identity` and `sexual_orientation` in `tblPerson`. Both fields are optional, can change over time, and are only visible to the profile owner.

## Gender Identity Options
1. Male
2. Female
3. Nonbinary
4. Other
5. Choose not to disclose

## Sexual Orientation Options
1. Heterosexual
2. Homosexual
3. Bisexual
4. Other
5. Questioning
6. Choose not to disclose

## Notes
- Both fields are private, never returned to anyone except the profile owner
- Both fields are optional, users can leave them blank
- The frontend should present these as dropdowns using the exact strings above
- The backend should accept any of the above strings or null/empty