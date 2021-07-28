#!/bin/bash

# Determine which of magpie-discovery, magpie-dmap, and magpie-policy we'll be running. Both is always an option and the default if
# not specified.

if [[ ${MAGPIE_DISCOVERY} == true ]] || [[ ${MAGPIE_POLICY:=false} == false && ${MAGPIE_DMAP:=false} == false ]] ;
then
  echo "Executing Magpie Discovery" >&2
  ./magpie-discovery "$@"
fi

if [[ ${MAGPIE_DMAP} == true ]] || [[ ${MAGPIE_DISCOVERY:=false} == false && ${MAGPIE_POLICY:=false} == false ]] ;
then
  echo "Executing Magpie DMAP" >&2
  ./magpie-dmap "$@"
fi

if [[ ${MAGPIE_POLICY} == true ]] || [[ ${MAGPIE_DISCOVERY:=false} == false && ${MAGPIE_DMAP:=false} == false ]] ;
then
  echo "Executing Magpie Policy" >&2
  ./magpie-policy "$@"
fi


