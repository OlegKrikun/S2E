#!/system/bin/sh

# Initialization, checks and mounts ####################################################################################
BB='busybox'

# Initialization
${BB} echo "S2E: Initialization..."

if [ "${SD_EXT_DIRECTORY}" = "" ];
then
	SD_EXT_DIRECTORY=/sd-ext
fi

S2E_CONFIG_DIR='/data/local/s2e_config/'
EXTPART='/dev/block/mmcblk0p2'

if [ -e "${S2E_CONFIG_DIR}.read_ahead" ]
then
    ${BB} cat ${S2E_CONFIG_DIR}.read_ahead > /sys/devices/virtual/bdi/179:0/read_ahead_kb
fi

if [ -e "${S2E_CONFIG_DIR}.mounts_ext4" ]
then
    if [ "`${BB} egrep -q ${SD_EXT_DIRECTORY} /proc/mounts; ${BB} echo $?`" = "0" ];
    then
        ${BB} umount ${SD_EXT_DIRECTORY}
    fi
    tune2fs -O extents,uninit_bg,dir_index ${EXTPART}
    e2fsck -yf ${EXTPART}
    tune2fs -o journal_data_writeback ${EXTPART}
    tune2fs -O ^has_journal ${EXTPART}
    ${BB} mount -t ext4 -o commit=19,barrier=0,nobh,nouser_xattr,errors=continue,noatime,nodiratime,nosuid,nodev,data=writeback ${EXTPART} ${SD_EXT_DIRECTORY}

fi

if [ "`${BB} egrep -q ${SD_EXT_DIRECTORY} /proc/mounts; ${BB} echo $?`" != "0" ];
then
    ${BB} echo "S2E: ${SD_EXT_DIRECTORY} not mounted... Exit!"
    exit
fi


if [ -e '/data/data/ru.krikun.s2e/shared_prefs/ru.krikun.s2e_preferences.xml' ];
then
    S2E_PREF='/data/data/ru.krikun.s2e/shared_prefs/ru.krikun.s2e_preferences.xml'
    S2E_STATUS='/data/data/ru.krikun.s2e/status'
    ${BB} echo "S2E: Config found on /data/data"
else
    if [ -e '/sd-ext/data/ru.krikun.s2e/shared_prefs/ru.krikun.s2e_preferences.xml' ];
    then
        S2E_PREF='/sd-ext/data/ru.krikun.s2e/shared_prefs/ru.krikun.s2e_preferences.xml'
        S2E_STATUS='/sd-ext/data/ru.krikun.s2e/status'
        ${BB} echo "S2E: Config found on /sd-ext/data"
    else
        ${BB} echo "S2E: Config not found... Exit!"
	    exit
    fi
fi

if [ ! -e ${S2E_STATUS} ];
then
    ${BB} mkdir  ${S2E_STATUS}
fi
if [ ! -L ${S2E_STATUS} ];
then
    ${BB} rm -rf ${S2E_STATUS}/*
fi

for dir in app app-private data dalvik-cache;
    do
    RESULT=`${BB} readlink /data/${dir} | ${BB} grep ${SD_EXT_DIRECTORY}`
    if [ ${RESULT} = "${SD_EXT_DIRECTORY}/${dir}" ]
        then
	        ${BB} rm -rf /data/${dir}
	        ${BB} mkdir /data/${dir}
    fi
done


# Moving items #########################################################################################################

# Apps and Private Apps
for dir in app app-private;
    do
    CONFIG=`${BB} grep -q "name=\"${dir}\" value=\"true\"" ${S2E_PREF}; ${BB} echo $?`
    if [ "${CONFIG}" = "0" ];
    then
        if [ "`${BB} egrep -q \"/data/${dir}\" /proc/mounts; ${BB} echo $?`" != "0" ];
        then
            if [ ! -e "${SD_EXT_DIRECTORY}/${dir}" ];
            then
                ${BB} mkdir ${SD_EXT_DIRECTORY}/${dir}
                ${BB} chown system:system ${SD_EXT_DIRECTORY}/${dir}
                ${BB} chmod 0771 ${SD_EXT_DIRECTORY}/${dir}
            fi
            if [ -L "/data/${dir}" ];
            then
                ${BB} chown system:system /data/${dir}
                ${BB} chmod 0771 /data/${dir}
            fi

            for app in `${BB} find "/data/${dir}" -type f -iname "*.apk" -o -iname "*.zip"`;
            do
                ${BB} mv ${app} ${SD_EXT_DIRECTORY}/${dir}/
            done

            ${BB} mount -o bind ${SD_EXT_DIRECTORY}/${dir}/ /data/${dir}

            if [ "`${BB} egrep -q \"/data/${dir}\" /proc/mounts; ${BB} echo $?`" = "0" ];
            then

                ${BB} echo "S2E: ${SD_EXT_DIRECTORY}/${dir} mount as /data/${dir}"
                ${BB} touch ${S2E_STATUS}/${dir}
            else
                ${BB} echo "S2E: ${SD_EXT_DIRECTORY}/${dir} not mount..."
            fi
        else
            ${BB} echo "S2E: ${SD_EXT_DIRECTORY}/${dir} already mount..."
        fi
    else
        if [ -e "${SD_EXT_DIRECTORY}/${dir}" ];
        then
            if [ ! -L "${SD_EXT_DIRECTORY}/${dir}" ];
            then
                for app in `${BB} find "${SD_EXT_DIRECTORY}/${dir}" -type f -iname "*.apk" -o -iname "*.zip"`;
                do
                    ${BB} mv ${app} /data/${dir}/
                done
            fi
            if [ -L "${SD_EXT_DIRECTORY}/${dir}" ];
            then
                ${BB} rm -rf ${SD_EXT_DIRECTORY}/${dir}
            fi
        fi
    fi
done

# Data
CONFIG=`${BB} grep -q "name=\"data\" value=\"true\"" ${S2E_PREF}; ${BB} echo $?`
if [ "${CONFIG}" = "0" ];
then
    if [ "`${BB} egrep -q \"/data/data\" /proc/mounts; ${BB} echo $?`" != "0" ];
    then
        if [ ! -e "${SD_EXT_DIRECTORY}/data" ];
        then
            ${BB} mkdir ${SD_EXT_DIRECTORY}/data
            ${BB} chown system:system ${SD_EXT_DIRECTORY}/data
            ${BB} chmod 0771 ${SD_EXT_DIRECTORY}/data
        fi
        if [ ! -L "/data/data" ];
        then
            ${BB} mv /data/data/* ${SD_EXT_DIRECTORY}/data
            ${BB} rm -rf /data/data/*
        fi

        ${BB} chown system:system /data/data
        ${BB} chmod 0771 /data/data
        ${BB} mount -o bind ${SD_EXT_DIRECTORY}/data/ /data/data

        if [ "`${BB} egrep -q \"/data/data\" /proc/mounts; ${BB} echo $?`" = "0" ];
        then

            ${BB} echo "S2E: ${SD_EXT_DIRECTORY}/data mount as /data/data"
            ${BB} touch ${S2E_STATUS}/data
        else
            ${BB} echo "S2E: ${SD_EXT_DIRECTORY}/data not mount..."
        fi
    else
        ${BB} echo "S2E: ${SD_EXT_DIRECTORY}/data already mount..."
    fi
else
    if [ -e "${SD_EXT_DIRECTORY}/data" ];
    then
        if [ ! -L "${SD_EXT_DIRECTORY}/data" ];
        then
            ${BB} mv ${SD_EXT_DIRECTORY}/data/* /data/data/
            ${BB} rm -rf ${SD_EXT_DIRECTORY}/data
        fi
        if [ -e '/data/data/ru.krikun.s2e/shared_prefs/ru.krikun.s2e_preferences.xml' ];
        then
            S2E_PREF='/data/data/ru.krikun.s2e/shared_prefs/ru.krikun.s2e_preferences.xml'
            S2E_STATUS='/data/data/ru.krikun.s2e/status'
            ${BB} echo "S2E: Config now on /data/data"
        fi
    fi
fi

# Dalvik-Cache
CONFIG=`${BB} grep -q "name=\"dalvik-cache\" value=\"true\"" ${S2E_PREF}; ${BB} echo $?`
if [ "${CONFIG}" = "0" ];
then
    if [ "`${BB} egrep -q \"/data/dalvik-cache\" /proc/mounts; ${BB} echo $?`" != "0" ];
    then
        if [ ! -e "${SD_EXT_DIRECTORY}/dalvik-cache" ];
        then
            ${BB} mkdir ${SD_EXT_DIRECTORY}/dalvik-cache
        fi
        if [ ! -L "/data/dalvik-cache" ];
        then
            ${BB} rm -rf /data/dalvik-cache
            ${BB} mkdir /data/dalvik-cache
        fi
        ${BB} chown system:system ${SD_EXT_DIRECTORY}/dalvik-cache
        ${BB} chmod 0771 ${SD_EXT_DIRECTORY}/dalvik-cache
        ${BB} chown system:system /data/dalvik-cache
        ${BB} chmod 0771 /data/dalvik-cache

        ${BB} mount -o bind ${SD_EXT_DIRECTORY}/dalvik-cache/ /data/dalvik-cache

        if [ "`${BB} egrep -q \"/data/dalvik-cache\" /proc/mounts; ${BB} echo $?`" = "0" ];
        then
            ${BB} echo "S2E: ${SD_EXT_DIRECTORY}/dalvik-cache mount as /data/dalvik-cache"
            ${BB} touch ${S2E_STATUS}/dalvik-cache
        else
            ${BB} echo "S2E: ${SD_EXT_DIRECTORY}/dalvik-cache not mount..."
        fi
    else
        ${BB} echo "S2E: ${SD_EXT_DIRECTORY}/dalvik-cache already mount..."
    fi
else
    if [ -e "${SD_EXT_DIRECTORY}/dalvik-cache" ];
    then
        ${BB} rm -rf ${SD_EXT_DIRECTORY}/dalvik-cache
    fi
fi

# Download cache
CONFIG=`${BB} grep -q "name=\"download\" value=\"true\"" ${S2E_PREF}; ${BB} echo $?`
if [ "${CONFIG}" = "0" ];
then
    if [ "`${BB} egrep -q \"/cache/download\" /proc/mounts; ${BB} echo $?`" = "0" ];
    then
        ${BB} umount /cache/download
        ${BB} echo "S2E: unmount /cache/download..."
    fi
    if [ ! -e "${SD_EXT_DIRECTORY}/download" ];
    then
        ${BB} mkdir ${SD_EXT_DIRECTORY}/download
    fi
    if [ ! -L "/cache/download" ];
    then
        ${BB} rm -rf /cache/download
        ${BB} mkdir /cache/download
    fi
    ${BB} chown system:cache ${SD_EXT_DIRECTORY}/download
    ${BB} chmod 0777 ${SD_EXT_DIRECTORY}/download
    ${BB} chown system:cache /cache/download
    ${BB} chmod 0777 /cache/download

    ${BB} mount -o bind ${SD_EXT_DIRECTORY}/download/ /cache/download

    if [ "`${BB} egrep -q \"/cache/download\" /proc/mounts; ${BB} echo $?`" = "0" ];
    then
        ${BB} echo "S2E: ${SD_EXT_DIRECTORY}/download mount as /cache/download"
        ${BB} touch ${S2E_STATUS}/download
    else
        ${BB} echo "S2E: ${SD_EXT_DIRECTORY}/download not mount..."
    fi
else
    if [ -e "${SD_EXT_DIRECTORY}/download" ];
    then
        ${BB} rm -rf ${SD_EXT_DIRECTORY}/download
    fi
fi

# Finish ###############################################################################################################
${BB} echo "S2E: Done!"
