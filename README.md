# ARLocationNavigator
appoly/ARCore-LocationとGoogleMapを使った位置情報アプリです。

<img src="https://github.com/hroabe/ARLocationNavigator/blob/master/app/src/main/assets/screenshot/cap.jpg" height=550 />

## 使用方法
Google MAP API キーを取得して、下記のファイルにセットする必要があります。
ARLocationNavigator/app/src/main/res/values/map_key.xml



```
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="map_key" translatable="false">ここにキーを入れます</string>
</resources>
```

また、Google Cloud Platformにて、下記のAPIを有効にする必要があります。
- Maps Elevation API
- Maps SDK for Android
