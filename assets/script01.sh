#!/system/bin/sh

# Initialization and checks
echo "S2E: Initialization..."

S2E_PREF='/data/data/ru.krikun.s2e/shared_prefs/ru.krikun.s2e_preferences.xml'
S2E_STATUS='/data/data/ru.krikun.s2e/status'

if [ "$SD_EXT_DIRECTORY" = "" ];
then
	SD_EXT_DIRECTORY=/sd-ext
fi
if [ "`egrep -q $SD_EXT_DIRECTORY /proc/mounts;echo $?`" != "0" ];
then
	echo "S2E: $SD_EXT_DIRECTORY not mounted... Exit!"
	exit
fi
if [ ! -e $S2E_STATUS ];
then
    mkdir  $S2E_STATUS
fi

# Apps and Private Apps
for dir in app app-private;
    do
    CONFIG=`grep -q "name=\"$dir\" value=\"true\"" $S2E_PREF;echo $?`
    if [ "$CONFIG" = "0" ];
    then
        if [ "`egrep -q \"/data/$dir\" /proc/mounts;echo $?`" != "0" ];
        then
            if [ ! -e "$SD_EXT_DIRECTORY/$dir" ];
            then
                mkdir $SD_EXT_DIRECTORY/$dir
                chown system:system $SD_EXT_DIRECTORY/$dir
                chmod 0771 $SD_EXT_DIRECTORY/$dir
            fi
            if [ -L "/data/$dir" ];
            then
                chown system:system /data/$dir
                chmod 0771 /data/$dir
            fi

            for app in `find "/data/$dir" -type f -iname "*.apk" -o -iname "*.zip"`;
            do
                mv $app $SD_EXT_DIRECTORY/$dir/
            done

            mount -o bind $SD_EXT_DIRECTORY/$dir/ /data/$dir

            if [ "`egrep -q \"/data/$dir\" /proc/mounts;echo $?`" = "0" ];
            then
                echo "S2E: $SD_EXT_DIRECTORY/$dir mount as /data/$dir"
                touch $S2E_STATUS/$dir
            else
                echo "S2E: $SD_EXT_DIRECTORY/$dir not mount..."
                if [ -e $S2E_STATUS/$dir ];
                then
                    rm -f  $S2E_STATUS/$dir
                fi
            fi
        else
            echo "S2E: $SD_EXT_DIRECTORY/$dir already mount..."
            if [ -e $S2E_STATUS/$dir ];
            then
                rm -f  $S2E_STATUS/$dir
            fi
        fi
    fi
done

# Dalvik-Cache
CONFIG=`grep -q "name=\"dalvik-cache\" value=\"true\"" $S2E_PREF;echo $?`
if [ "$CONFIG" = "0" ];
then
    if [ "`egrep -q \"/data/dalvik-cache\" /proc/mounts;echo $?`" != "0" ];
    then
        if [ ! -e "$SD_EXT_DIRECTORY/dalvik-cache" ];
        then
            mkdir $SD_EXT_DIRECTORY/dalvik-cache
        fi
        if [ ! -L "/data/dalvik-cache" ];
        then
            rm -rf /data/dalvik-cache
            mkdir /data/dalvik-cache
        fi
        chown system:system $SD_EXT_DIRECTORY/dalvik-cache
        chmod 0771 $SD_EXT_DIRECTORY/dalvik-cache
        chown system:system /data/dalvik-cache
        chmod 0771 /data/dalvik-cache

        mount -o bind $SD_EXT_DIRECTORY/dalvik-cache/ /data/dalvik-cache

        if [ "`egrep -q \"/data/dalvik-cache\" /proc/mounts;echo $?`" = "0" ];
        then
            echo "S2E: $SD_EXT_DIRECTORY/dalvik-cache mount as /data/dalvik-cache"
            touch $S2E_STATUS/dalvik-cache
        else
            echo "S2E: $SD_EXT_DIRECTORY/dalvik-cache not mount..."
            if [ -e $S2E_STATUS/dalvik-cache ];
            then
                rm -f  $S2E_STATUS/dalvik-cache
            fi
        fi
    else
        echo "S2E: $SD_EXT_DIRECTORY/dalvik-cache already mount..."
        if [ -e $S2E_STATUS/dalvik-cache ];
        then
            rm -f  $S2E_STATUS/dalvik-cache
        fi
    fi
fi

# Download cache
CONFIG=`grep -q "name=\"download\" value=\"true\"" $S2E_PREF;echo $?`
if [ "$CONFIG" = "0" ];
then
    if [ "`egrep -q \"/cache/download\" /proc/mounts;echo $?`" = "0" ];
    then
        umount /cache/download
        echo "S2E: /cache/download unmounted..."
    fi
    if [ ! -e "$SD_EXT_DIRECTORY/download" ];
    then
        mkdir $SD_EXT_DIRECTORY/download
    fi
    if [ ! -L "/cache/download" ];
    then
        rm -rf /cache/download
        mkdir /cache/download
    fi
    chown system:cache $SD_EXT_DIRECTORY/download
    chmod 0771 $SD_EXT_DIRECTORY/download
    chown system:cache /cache/download
    chmod 0771 /cache/download

    mount -o bind $SD_EXT_DIRECTORY/download/ /cache/download

    if [ "`egrep -q \"/cache/download\" /proc/mounts;echo $?`" = "0" ];
    then
        echo "S2E: $SD_EXT_DIRECTORY/download mount as /cache/download"
        touch $S2E_STATUS/download
    else
        echo "S2E: $SD_EXT_DIRECTORY/download not mount..."
        if [ -e $S2E_STATUS/download ];
        then
            rm -f  $S2E_STATUS/download
        fi
    fi
fi

# Finish
echo "S2E: Done!"
