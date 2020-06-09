print_err() {
  if [ "$1" -ne 0 ]; then echo "$2" | grep "not ok"; fi
}