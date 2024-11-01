# Android_PDR

## Summary

You can obtain data from the linear acceleration sensor and gyroscope of an Android smartphone to perform Pedestrian Dead Reckoning (PDR) without GPS.

I developed it to contribute my article to a book for the techbookfest (技術書典17).


## Usage

1. Build and install the [SpyApp](SpyApp) on your Android phone.
2. Walk with the app and get the data.
3. Feed the collected data into the script, [SensorDataToMap.py](SensorDataToMap.py).
4. Input some information such as initial positions or the subject (pedestrian)'s height to the script.
5. Install the required packages from [requirements.txt](requirements.txt), then analyze the data using the Python script.
6. Obtain a map showing the trajectory of the walk.

Here are some guidelines while walking:
- Only move on flat surfaces.
- Do not run.
- Walk with consistent stride length.
- Hold the smartphone as level as possible with the screen facing upward, keeping the top of the screen pointing in the direction of movement. Press the bottom of the screen against the center of your abdomen.
- Do not change your body orientation while stationary.


## Languages Used
- Kotlin 1.9.0
- Python 3.9.6

## Acknowledgements
- This project utilizes software that is under some licenses.
    - [NOTICE.md](NOTICE.md)