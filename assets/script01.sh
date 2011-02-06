#!/system/bin/sh
echo "S2E: Initialization..."

S2E_PREF='/data/data/ru.krikun.simple2ext/shared_prefs/ru.krikun.simple2ext_preferences.xml'
S2E_STATUS='/data/data/ru.krikun.simple2ext/status'

if [ "$SD_EXT_DIRECTORY" = "" ];
then
	SD_EXT_DIRECTORY=/sd-ext
fi

if [ ! -e $S2E_STATUS ];
then
    mkdir  $S2E_STATUS
fi

for dir in app app-private dalvik-cache download;
    do
    CONFIG=`grep -q "name=\"$dir\" value=\"true\"" $S2E_PREF;echo $?`
    if [ "$CONFIG" = "0" ];
    then
        if [ $dir = "download" ];
        then
            SRC_PART=/cache
            if [ "`egrep -q \"$SRC_PART/$dir\" /proc/mounts;echo $?`" = "0" ];
            then
                umount $SRC_PART/$dir
            fi
        else
            SRC_PART=/data
        fi
        if [ "`egrep -q \"$SRC_PART/$dir\" /proc/mounts;echo $?`" != "0" ];
        then
            if [ ! -e "$SD_EXT_DIRECTORY/$dir" ];
            then
                install -m 771 -o 1000 -g 1000 -d $SD_EXT_DIRECTORY/$dir
            fi
            if [ $dir = "dalvik-cache" -o $dir = "download" ];
            then
                if [ ! -L "$SRC_PART/$dir" ];
                then
                    rm -rf $SRC_PART/$dir
                    install -m 771 -o 1000 -g 1000 -d $SRC_PART/$dir
                fi
            fi
            if [ -L "$SRC_PART/$dir" ];
            then
                if [ $dir = "download" ];
                then
                    chown 1000:1000 $SRC_PART/$dir
                    chmod 0777 $SRC_PART/$dir
                else
                    chown 1000:1000 $SRC_PART/$dir
                    chmod 0771 $SRC_PART/$dir
                fi
            fi
            if [ $dir = "app" -o $dir = "app-private" ];
            then
                for app in `find "$SRC_PART/$dir" -type f -iname "*.apk" -o -iname "*.zip"`;
                do
                    mv $app $SD_EXT_DIRECTORY/$dir/
                done
            fi
            mount -o bind $SD_EXT_DIRECTORY/$dir/ $SRC_PART/$dir
        fi
        if [ "`egrep -q \"$SRC_PART/$dir\" /proc/mounts;echo $?`" = "0" ];
        then
            echo "S2E: $SD_EXT_DIRECTORY/$dir mount as $SRC_PART/$dir"
            touch $S2E_STATUS/$dir
        else
            echo "S2E: $SD_EXT_DIRECTORY/$dir not mount..."
            if [ -e $S2E_STATUS/$dir ];
            then
                rm -f  $S2E_STATUS/$dir
            fi
        fi
    fi
done
echo "S2E: Done!"
