#ifndef _NULL_ZOOM_CONTROLLER_H
#define _NULL_ZOOM_CONTROLLER_H

#include "NXZoomController.h"

namespace android {

class NullZoomController: public NXZoomController
{
public:
    NullZoomController() {
    }
    virtual ~NullZoomController() {
    }

    // overriding
    virtual void setBase(__attribute__((__unused__)) int baseWidth,
			 __attribute__((__unused__)) int baseHeight) {
    }
    virtual void setCrop(__attribute__((__unused__)) int left,
			 __attribute__((__unused__)) int top,
			 __attribute__((__unused__)) int width,
			 __attribute__((__unused__)) int height) {
    }
    virtual void setFormat(__attribute__((__unused__)) int srcFormat,
			   __attribute__((__unused__)) int dstFormat) {
    }
    virtual bool handleZoom(__attribute__((__unused__)) struct nxp_vid_buffer *srcBuffer,
			    __attribute__((__unused__)) private_handle_t const *dstHandle) {
        return true;
    }
    virtual bool handleZoom(__attribute__((__unused__)) struct nxp_vid_buffer *srcBuffer,
			    __attribute__((__unused__)) struct nxp_vid_buffer *dstBuffer) {
        return true;
    }
    virtual bool handleZoom(__attribute__((__unused__)) private_handle_t const *srcHandle,
			    __attribute__((__unused__)) struct nxp_vid_buffer *dstBuffer) {
        return true;
    }
    virtual bool handleZoom(__attribute__((__unused__)) private_handle_t const *srcHandle,
			    __attribute__((__unused__)) private_handle_t const *dstHandle) {
        return true;
    }
    virtual bool isZoomAvaliable() {
        return false;
    }
    virtual bool allocBuffer(__attribute__((__unused__)) int bufferCount,
			     __attribute__((__unused__)) int width,
			     __attribute__((__unused__)) int height,
			     __attribute__((__unused__)) int format) {
        return true;
    }
    virtual void freeBuffer() {
    }
    virtual struct nxp_vid_buffer *getBuffer(__attribute__((__unused__)) int index) {
        return NULL;
    }
    virtual bool useZoom() {
        return false;
    }
};

}; // namespace

#endif
