#!/bin/bash

#echo "date,time,lat,lon,ele,desc,speed,cour" > newfile.ucsv; grep ^LU malcgps.log  | cut -d":" -f2- | sed "s/NULL/0.0/g" >>newfile.ucsv ; gpsbabel -r -i unicsv -f newfile.ucsv -x position,distance=20m -o gpx -F gpsRoute.gpx
echo "date,time,lat,lon,ele,desc,speed,cour" > newfile.ucsv; grep ^LU malcgps.log  | cut -d":" -f2- | sed "s/NULL/0.0/g" >>newfile.ucsv ; gpsbabel -r -i unicsv -f newfile.ucsv -x position,distance=5m -o gpx -F gpsRoute.gpx
gpsbabel -i gpx -f gpsRoute.gpx -o kml -F gpsRoute.kml
gpsbabel -i gpx -f gpsRoute.gpx -x transform,wpt=rte -x transform,trk=wpt -o gpx -F gpsTrack.gpx

