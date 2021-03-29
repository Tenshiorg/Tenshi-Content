:: automatic release using git flow release
:: usage: make-release <RELEASE_TAG>

@echo off
:: check param 1 is set
IF "%~1" == "" (
    echo No RELEASE_TAG specified.
    goto:eof
    exit 1
)

:: user confirm
echo Will create a release %~1
pause

:: start a new release
git flow release start %~1

:: checkout release branch on submodule extension-lib
git submodule update --init --recursive --remote
pushd .\Extensions-Lib\
git checkout senpai
popd

:: pause for last- minute changes
echo.
echo pausing now. do your last- minute changes now or undo with 'git flow release delete %~1'
echo checklist:
echo [ ] Project Compiles correctly
echo [ ] Tag %~1 does not already exist
echo [ ] Switch Extensions-Lib to senpai branch (auto)
echo [ ] Set Version in build.gradle
echo [ ] WebAdapter: Constants adjusted (DEBUG_MODE false, BASE_URL set to release repo)
pause
pause

:: finish the release
git flow release finish %~1

:: push all branches
git push --all

:: push tags to remote (triggers auto- build)
git push --tags