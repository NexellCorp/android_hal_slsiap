#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <linux/fb.h>
#include <cutils/log.h>

#include <NXUtil.h>

int getScreenAttribute(const char *fbname, int32_t &xres, int32_t &yres, int32_t &refreshRate)
{
    int ret = 0;
    char *path;
    char buf[128] = {0, };
    int fd;
    unsigned int x, y, r;

    asprintf(&path, "/sys/class/graphics/%s/modes", fbname);
    if (!path) {
        ALOGE("%s: failed to asprintf()", __func__);
        return -EINVAL;
    }

    fd = open(path, O_RDONLY);
    if (fd < 0) {
        ALOGE("%s: failed to open %s", __func__, path);
        ret = -EINVAL;
        goto err_open;
    }

    ret = read(fd, buf, sizeof(buf));
    if (ret <= 0) {
         ALOGE("%s: failed to read %s", __func__, path);
         ret = -EINVAL;
         goto err_read;
    }

    ret = sscanf(buf, "U:%ux%up-%u", &x, &y, &r);
    if (ret != 3) {
         ALOGE("%s: failed to sscanf()", __func__);
         ret = -EINVAL;
         goto err_sscanf;
    }

    ALOGI("Using %ux%u %uHz resolution for %s from modes list\n", x, y, r, fbname);

    xres = (int32_t)x;
    yres = (int32_t)y;
    refreshRate = (int32_t)r;

    close(fd);
    free(path);
    return 0;

err_sscanf:
err_read:
    close(fd);
err_open:
    free(path);

    return ret;
}
