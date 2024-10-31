import numpy as np
import csv
from scipy.signal import find_peaks
import math
import folium

data_acc = []
data_timestamp = []

# 直線加速度センサーのデータを読み込む
with open('./lin_acc_data_dddddddddd.csv') as f:
    reader = csv.reader(f)
    header = next(reader)  # ヘッダーをスキップ
    for row in reader:
        data_acc.append(float(row[1]))  # 2列目の値を取得してリストに追加
        data_timestamp.append(int(row[3]))  # 4列目の値を取得してリストに追加

# 加速度データの数を取得
data_row_count = len(data_acc)

# 加速度データをNumPy配列に変換
data_acc = np.array(data_acc)

# データの正負を反転してピークを探し、ピークのインデックスを格納
neg_peaks_indexes, _ = find_peaks(-data_acc)

# 負のピークの大きさでフィルタリングし、-3.0以下のピークのインデックスのみを格納
filtered_neg_peaks_indexes = [peak_index for peak_index in neg_peaks_indexes if data_acc[peak_index] <= -3.0]

# フィルタリングしたピークのインデックスをスカラー値として格納
peak_indexes = []
for i in range(len(filtered_neg_peaks_indexes)):
    peak_indexes.append(filtered_neg_peaks_indexes[i].item())

# ピーク間のインデックスの数の差を求める
peak_index_diffs = []
for i in range(len(peak_indexes) - 1):
    peak_index_diffs.append(peak_indexes[i+1] - peak_indexes[i])

# ピーク間のインデックスの差を昇順に並べ、ざっくりとした中央値を取る
sorted_peak_index_diffs = sorted(peak_index_diffs)
length = len(sorted_peak_index_diffs)
peak_index_diff_median = sorted_peak_index_diffs[length//2]

# データ利用フラグを持つリストを用意し初期化
enabled_flags = [0] * data_row_count

# ピークの位置に利用フラグを立てる
for i in peak_indexes:
    enabled_flags[i] = 1

# ピークの前後に利用フラグを立てる
for peak_i in peak_indexes:
    for med_i in range(math.floor(peak_index_diff_median * 1.5)):
        try:
            enabled_flags[peak_i - med_i] = 1
        except IndexError:
            pass
    for med_i in range(math.floor(peak_index_diff_median * 0.5)):
        try:
            enabled_flags[peak_i + med_i] = 1
        except IndexError:
            pass

# フラグが立っているセグメントの最初と最後のインデックスのペアをリストに格納
start_end_indexes = []
is_termination = 0
starting_point_index = 0
for i in range(data_row_count):
    if enabled_flags[i] == 1 and is_termination == 0:
        starting_point_index = i
        is_termination = int(not is_termination)
        continue
    elif (enabled_flags[i] == 0 and is_termination == 1):
        start_end_indexes.append([starting_point_index, i - 1])
        is_termination = int(not is_termination)
    elif (i == data_row_count - 1) and is_termination == 1:
        start_end_indexes.append([starting_point_index, i])

# インデックスに対応するtimestampを取得
start_end_timestamps = []
for i in range(len(start_end_indexes)):
    start_end_timestamps.append([data_timestamp[start_end_indexes[i][0]], data_timestamp[start_end_indexes[i][1]]])


data_gyro = []
gyro_timestamps = []
sampling_rates = []

# ジャイロセンサーのデータを読み込む
with open('./gyro_data_dddddddddd.csv') as f:
    reader = csv.reader(f)
    header = next(reader)  # ヘッダーをスキップ
    for row in reader:
        data_gyro.append(float(row[0]))  # 1列目の値を取得してリストに追加
        gyro_timestamps.append(float(row[1]))  # 2列目の値を取得してリストに追加
        sampling_rates.append(float(row[2]))  # 3列目の値を取得してリストに追加

flag_end = 0
enabled_gyro_data = []
enabled_sampling_rates = []

# 歩行中のジャイロセンサーのデータのみ抽出
interval = 0
is_stationary = 1
for i in range(len(gyro_timestamps)):
    if is_stationary == 0:
        if gyro_timestamps[i] >= start_end_timestamps[interval][0] and gyro_timestamps[i] <= start_end_timestamps[interval][1]:
            enabled_gyro_data.append(data_gyro[i])
            enabled_sampling_rates.append(sampling_rates[i])
        elif gyro_timestamps[i] > start_end_timestamps[interval][1]:
            if interval + 1 == len(start_end_timestamps):
                break
            else:
                interval += 1
                is_stationary = 1
    else:
        if gyro_timestamps[i] >= start_end_timestamps[interval][0]:
            is_stationary = 0


# 十進法形式のスタート地点の緯度と経度
start_lat = 00.00000000 # input your starting latitude
start_lon = 000.00000000    # input your starting longitude

# スタート地点で向いている左手系北基準の方位角[rad]
angle = 0.00    # input your starting azimuth angle

# 初期設定
lat, lon = start_lat, start_lon
route = [(lat, lon)]
TARGET_HEIGHT_METERS = 0.00 # input value in meters
STEP_LENGTH_METERS = TARGET_HEIGHT_METERS * 0.45

# 新しい位置を計算
for i in range(len(enabled_gyro_data)):
    if enabled_sampling_rates[i] > 1:
        angle += enabled_gyro_data[i] * -1 / enabled_sampling_rates[i]
    else:
        continue
    distance = STEP_LENGTH_METERS / peak_index_diff_median

    # 位置を更新
    lat += distance * np.cos(angle) / 111000
    lon += distance * np.sin(angle) / (111000 * np.cos(np.radians(lat)))
    route.append((lat, lon))

# Foliumで地図を作成
m = folium.Map(location=[start_lat, start_lon], zoom_start=18)

# # スタート地点のサークルマーカーのプロット
# folium.Marker(
#     location=[start_lat, start_lon],
#     popup='start',
#     icon=folium.Icon(color='blue')
# ).add_to(m)

# ルートを地図に追加
folium.PolyLine(route, color="blue").add_to(m)

# 推定したゴール地点のサークルマーカーのプロット
folium.Marker(
    location=[lat, lon],
    popup='estimated_goal',
    icon=folium.Icon(color='red')
).add_to(m)

# 実際のゴール地点のサークルマーカーのプロット
folium.Marker(
    location=[00.00000000000000, 000.00000000000000],   # input actual latitude and longitude
    popup='actual_goal',
    icon=folium.Icon(color='green')
).add_to(m)

# 地図を保存
m.save("result_map.html")