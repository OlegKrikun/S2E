#!/system/bin/sh

# Initialization, checks and mounts ####################################################################################

# Initialization
echo "S2E: Initialization..."

if [ "$SD_EXT_DIRECTORY" = "" ];
then
	SD_EXT_DIRECTORY=/sd-ext
fi

S2E_CONFIG_DIR='/data/local/s2e_config/'
EXTPART='/dev/block/mmcblk0p2'


if [ -e "$S2E_CONFIG_DIR.read_ahead" ]
then
    cat $S2E_CONFIG_DIR.read_ahead > /sys/devices/virtual/bdi/179:0/read_ahead_kb
fi

if [ -e "$S2E_CONFIG_DIR.mounts_ext4" ]
then
    if [ "`egrep -q $SD_EXT_DIRECTORY /proc/mounts;echo $?`" = "0" ];
    then
        umount $SD_EXT_DIRECTORY
    fi
    tune2fs -O extents,uninit_bg,dir_index $EXTPART
    e2fsck -yf $EXTPART
    tune2fs -o journal_data_writeback $EXTPART
    tune2fs -O ^has_journal $EXTPART
    mount -t ext4 -o commit=19,barrier=0,nobh,nouser_xattr,errors=continue,noatime,nodiratime,nosuid,nodev,data=writeback $EXTPART $SD_EXT_DIRECTORY

fi

if [ "`egrep -q $SD_EXT_DIRECTORY /proc/mounts;echo $?`" != "0" ];
then
    echo "S2E: $SD_EXT_DIRECTORY not mounted... Exit!"
    exit
fi


if [ -e '/data/data/ru.krikun.s2e/shared_prefs/ru.krikun.s2e_preferences.xml' ];
then
    S2E_PREF='/data/data/ru.krikun.s2e/shared_prefs/ru.krikun.s2e_preferences.xml'
    S2E_STATUS='/data/data/ru.krikun.s2e/status'
    echo "S2E: Config found on /data/data"
else
    if [ -e '/sd-ext/data/ru.krikun.s2e/shared_prefs/ru.krikun.s2e_preferences.xml' ];
    then
        S2E_PREF='/sd-ext/data/ru.krikun.s2e/shared_prefs/ru.krikun.s2e_preferences.xml'
        S2E_STATUS='/sd-ext/data/ru.krikun.s2e/status'
        echo "S2E: Config found on /sd-ext/data"
    else
        echo "S2E: Config not found... Exit!"
	    exit
    fi
fi

if [ ! -e $S2E_STATUS ];
then
    mkdir  $S2E_STATUS
fi
if [ ! -L $S2E_STATUS ];
then
    rm -rf $S2E_STATUS/*
fi

# Moving items #########################################################################################################

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
            fi
        else
            echo "S2E: $SD_EXT_DIRECTORY/$dir already mount..."
        fi
    else
        if [ -e "$SD_EXT_DIRECTORY/$dir" ];
        then
            if [ ! -L "$SD_EXT_DIRECTORY/$dir" ];
            then
                for app in `find "$SD_EXT_DIRECTORY/$dir" -type f -iname "*.apk" -o -iname "*.zip"`;
                do
                    mv $app /data/$dir/
                done
            fi
            if [ -L "$SD_EXT_DIRECTORY/$dir" ];
            then
                rm -rf $SD_EXT_DIRECTORY/$dir
            fi
        fi
    fi
done

# Data
CONFIG=`grep -q "name=\"data\" value=\"true\"" $S2E_PREF;echo $?`
if [ "$CONFIG" = "0" ];
then
    if [ "`egrep -q \"/data/data\" /proc/mounts;echo $?`" != "0" ];
    then
        if [ ! -e "$SD_EXT_DIRECTORY/data" ];
        then
            mkdir $SD_EXT_DIRECTORY/data
            chown system:system $SD_EXT_DIRECTORY/data
            chmod 0771 $SD_EXT_DIRECTORY/data
        fi
        if [ ! -L "/data/data" ];
        then
            mv /data/data/* $SD_EXT_DIRECTORY/data
            rm -rf /data/data/*
        fi

        chown system:system /data/data
        chmod 0771 /data/data
        mount -o bind $SD_EXT_DIRECTORY/data/ /data/data

        if [ "`egrep -q \"/data/data\" /proc/mounts;echo $?`" = "0" ];
        then

            echo "S2E: $SD_EXT_DIRECTORY/data mount as /data/data"
            touch $S2E_STATUS/data
        else
            echo "S2E: $SD_EXT_DIRECTORY/data not mount..."
        fi
    else
        echo "S2E: $SD_EXT_DIRECTORY/data already mount..."
    fi
else
    if [ -e "$SD_EXT_DIRECTORY/data" ];
    then
        if [ ! -L "$SD_EXT_DIRECTORY/data" ];
        then
            mv $SD_EXT_DIRECTORY/data/* /data/data/
            rm -rf $SD_EXT_DIRECTORY/data
        fi
        if [ -e '/data/data/ru.krikun.s2e/shared_prefs/ru.krikun.s2e_preferences.xml' ];
        then
            S2E_PREF='/data/data/ru.krikun.s2e/shared_prefs/ru.krikun.s2e_preferences.xml'
            S2E_STATUS='/data/data/ru.krikun.s2e/status'
            echo "S2E: Config now on /data/data"
        fi
    fi
fi

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
        fi
    else
        echo "S2E: $SD_EXT_DIRECTORY/dalvik-cache already mount..."
    fi
else
    if [ -e "$SD_EXT_DIRECTORY/dalvik-cache" ];
    then
        rm -rf $SD_EXT_DIRECTORY/dalvik-cache
    fi
fi

# Download cache
CONFIG=`grep -q "name=\"download\" value=\"true\"" $S2E_PREF;echo $?`
if [ "$CONFIG" = "0" ];
then
    if [ "`egrep -q \"/cache/download\" /proc/mounts;echo $?`" = "0" ];
    then
        umount /cache/download
        echo "S2E: unmount /cache/download..."
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
    chmod 0777 $SD_EXT_DIRECTORY/download
    chown system:cache /cache/download
    chmod 0777 /cache/download

    mount -o bind $SD_EXT_DIRECTORY/download/ /cache/download

    if [ "`egrep -q \"/cache/download\" /proc/mounts;echo $?`" = "0" ];
    then
        echo "S2E: $SD_EXT_DIRECTORY/download mount as /cache/download"
        touch $S2E_STATUS/download
    else
        echo "S2E: $SD_EXT_DIRECTORY/download not mount..."
    fi
else
    if [ -e "$SD_EXT_DIRECTORY/download" ];
    then
        rm -rf $SD_EXT_DIRECTORY/download
    fi
fi

# Finish ###############################################################################################################
echo "S2E: Done!"
