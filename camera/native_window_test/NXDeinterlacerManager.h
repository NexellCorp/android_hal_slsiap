#ifndef _NX_DEINTERLACER_MANAGER_H
#define _NX_DEINTERLACER_MANAGER_H

#include <system/window.h>
#include <NXAllocator.h>
#include <NXQueue.h>
#include "nxp-deinterlacer.h"

namespace android {

class NXDeinterlacerManager
{
public:
    NXDeinterlacerManager(int srcWidth, int srcHeight);
    virtual ~NXDeinterlacerManager();

    virtual bool qSrcBuf(int index, struct nxp_vid_buffer *buf);
    virtual bool dqSrcBuf(int *pIndex, struct nxp_vid_buffer **pBuf);
    virtual bool qDstBuf(ANativeWindowBuffer *buf);
    virtual bool dqDstBuf(ANativeWindowBuffer **pBuf);
    virtual bool run(void);
    virtual int  getRunCount(void) {
        return mRunCount;
    }

private:
    struct SrcBufferType {
        int index;
        struct nxp_vid_buffer *buf;
    };

    enum {
        FIELD_EVEN = 0,
        FIELD_ODD
    };

    int mSrcWidth;
    int mSrcHeight;

    class NXQueue<SrcBufferType> mSrcBufferQueue;
    class NXQueue<ANativeWindowBuffer *> mDstBufferQueue;

    int mHandle;
    int mRunCount;
    int mIonHandle;

    frame_data_info mFrameInfo;
    int mCurrentField;

private:
    void makeFrameInfo(int index);
};

};
#endif
