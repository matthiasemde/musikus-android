grep -E ") - |http" app/build/reports/licenses/licenseReleaseReport.txt | sed 's/^ *//; s/ (.*//; s|\(http[^ ]*\)|(\1)|g' | sed ':a;N;$!ba;s/\n(http/ (http/g' | sed 's/^/* /' | awk '!seen[$0]++' > app/src/main/res/raw/third_party_licenses