#define	LOG_TAG				"NX_HEVCDEC"

#include <utils/Log.h>

#include <assert.h>
#include <OMX_AndroidTypes.h>
#include <system/graphics.h>

#include "NX_OMXVideoDecoder.h"
#include "NX_DecoderUtil.h"

//
//	TODO : Port Reconfiguration for HEVC Codec
//

typedef struct {
	int				profile;
	int				level;
	int				nal_length_size;
	int				num_cfg;
	int				cfg_length[6];
	unsigned char	cfg_data[6][1024];
} HVC1_TYPE;

static int NX_ParseHEVCCfgFromHVC1( unsigned char *extraData, int extraDataSize, HVC1_TYPE *hevcInfo )
{
	uint8_t *ptr = (uint8_t *)extraData;

	// verify minimum size and configurationVersion == 1.
	if (extraDataSize < 7 || ptr[0] != 1) {
		return -1;
	}

	hevcInfo->profile = (ptr[1] & 31);
	hevcInfo->level = ptr[12];
	hevcInfo->nal_length_size = 1 + (ptr[14 + 7] & 3);

	ptr += 22;
	extraDataSize -= 22;

	int numofArrays = (char)ptr[0];

	ptr += 1;
	extraDataSize -= 1;
	int j = 0, i = 0, cnt = 0;

	for (i = 0; i < numofArrays ; i++)
	{
		ptr += 1;
		extraDataSize -= 1;

		// Num of nals
		int numofNals = ptr[0] << 8 | ptr[1];

		ptr += 2;
		extraDataSize -= 2;

		for (j = 0 ; j < numofNals ; j++) {
			if (extraDataSize < 2) {
				return -1;
			}

			int length = ptr[0] << 8 | ptr[1];

			ptr += 2;
			extraDataSize -= 2;

			if (extraDataSize < length) {
				return -1;
			}

			hevcInfo->cfg_length[cnt] = length;
			memcpy( hevcInfo->cfg_data[cnt], ptr, length );

			ptr += length;
			extraDataSize -= length;
			cnt += 1;
		}
	}

	hevcInfo->num_cfg = cnt;
	return 0;
}

static void NX_MakeHEVCStreamHVC1ANNEXB( HVC1_TYPE *hvc1, unsigned char *pBuf, int *size )
{
	int i;
	int pos = 0;

	for( i=0 ; i<hvc1->num_cfg ; i++ )
	{
		pBuf[pos++] = 0x00;
		pBuf[pos++] = 0x00;
		pBuf[pos++] = 0x00;
		pBuf[pos++] = 0x01;
		memcpy( pBuf + pos, hvc1->cfg_data[i], hvc1->cfg_length[i] );
		pos += hvc1->cfg_length[i];
	}
	*size = pos;
}


int NX_DecodeHevcFrame(NX_VIDDEC_VIDEO_COMP_TYPE *pDecComp, NX_QUEUE *pInQueue, NX_QUEUE *pOutQueue)
{
	OMX_BUFFERHEADERTYPE* pInBuf = NULL, *pOutBuf = NULL;
	int inSize = 0;
	OMX_BYTE inData;
	NX_VID_DEC_IN decIn;
	NX_VID_DEC_OUT decOut;
	int ret = 0;

	UNUSED_PARAM(pOutQueue);

	memset(&decIn,  0, sizeof(decIn)  );

	if( pDecComp->bFlush )
	{
		flushVideoCodec( pDecComp );
		pDecComp->bFlush = OMX_FALSE;
	}

	//	Get Next Queue Information
	NX_PopQueue( pInQueue, (void**)&pInBuf );
	if ( pInBuf == NULL ){
		return 0;
	}

	inData = pInBuf->pBuffer;
	inSize = pInBuf->nFilledLen;
	pDecComp->inFrameCount++;


	TRACE("pInBuf->nFlags = 0x%08x, size = %ld\n", (int)pInBuf->nFlags, pInBuf->nFilledLen );
	//	Check End Of Stream
	if ( pInBuf->nFlags & OMX_BUFFERFLAG_EOS )
	{
		DbgMsg("=========================> Receive Endof Stream Message (%ld)\n", pInBuf->nFilledLen);
		pDecComp->bStartEoS = OMX_TRUE;
		if( inSize <= 0)
		{
			goto Exit;
		}
	}

	//	Step 1. Found Sequence Information
	if ( (OMX_TRUE == pDecComp->bNeedSequenceData) )
	{
		if ( pInBuf->nFlags & OMX_BUFFERFLAG_CODECCONFIG )
		{
			pDecComp->bNeedSequenceData = OMX_FALSE;
			DbgMsg("Copy Extra Data (%d)\n", inSize );

			//	TODO : Check Resolution Change
			if ( pDecComp->codecSpecificData )
				free( pDecComp->codecSpecificData );
			pDecComp->codecSpecificData = malloc(inSize);
			memcpy( pDecComp->codecSpecificData + pDecComp->codecSpecificDataSize, inData, inSize );
			pDecComp->codecSpecificDataSize += inSize;

			goto Exit;
		}
	}

	//	Push Input Time Stamp
	if ( !(pInBuf->nFlags & OMX_BUFFERFLAG_CODECCONFIG) )
		PushVideoTimeStamp(pDecComp, pInBuf->nTimeStamp, pInBuf->nFlags );

	//	Step 2. Find First Key Frame & Do Initialize VPU
	if ( OMX_FALSE == pDecComp->bInitialized )
	{
		int initBufSize;
		unsigned char *initBuf;

		if ( pDecComp->codecSpecificDataSize == 0 && pDecComp->nExtraDataSize>0 )
		{
			// check nal start code
			if ( (pDecComp->pExtraData[0]==0) && (pDecComp->pExtraData[1]==0) )
			{
				initBufSize = inSize + pDecComp->nExtraDataSize;
				initBuf = (unsigned char *)malloc( initBufSize );
				memcpy( initBuf, pDecComp->pExtraData, pDecComp->nExtraDataSize );
				memcpy( initBuf + pDecComp->nExtraDataSize, inData, inSize );
			}
			else
			{
				//	need hevc
				HVC1_TYPE hevcHeader;
				DbgMsg("~~~~~~~~~~~~~~~~~~~~~ HVC1 to NAL ~~~~~~~~~~~~~~~~~~~~~~\n");
				initBuf = (unsigned char *)malloc( pDecComp->nExtraDataSize + 1024 + inSize );
				NX_ParseHEVCCfgFromHVC1( pDecComp->pExtraData, pDecComp->nExtraDataSize, &hevcHeader );
				NX_MakeHEVCStreamHVC1ANNEXB( &hevcHeader, initBuf, &initBufSize );
				memcpy( initBuf + initBufSize, inData, inSize );
				initBufSize += inSize;
			}
		}
		else
		{
			initBufSize = inSize + pDecComp->codecSpecificDataSize;
			initBuf = (unsigned char *)malloc( initBufSize );
			memcpy( initBuf, pDecComp->codecSpecificData, pDecComp->codecSpecificDataSize );
			memcpy( initBuf + pDecComp->codecSpecificDataSize, inData, inSize );
		}

		//	Initialize VPU
		ret = InitializeCodaVpu(pDecComp, initBuf, initBufSize );
		free( initBuf );

		if ( 0 > ret )
		{
			ErrMsg("VPU initialized Failed!!!!\n");
			if (1)
			{
				uint8_t *tmp = inData;
				DbgMsg("0x%02x%02x%02x%02x 0x%02x%02x%02x%02x 0x%02x%02x%02x%02x 0x%02x%02x%02x%02x\n", 
					tmp[ 0], tmp[ 1], tmp[ 2], tmp[ 3], tmp[ 4], tmp[ 5], tmp[ 6], tmp[ 7],
					tmp[ 8], tmp[ 9], tmp[10], tmp[11], tmp[12], tmp[13], tmp[14], tmp[15] );
			}

			//	TimeStamp Dummy Pop
			if ( !(pInBuf->nFlags & OMX_BUFFERFLAG_CODECCONFIG) ){
				int64_t time;
				uint32_t flag;
				if( 0 != PopVideoTimeStamp(pDecComp, &time, &flag )  )
				{
					pOutBuf->nTimeStamp = pInBuf->nTimeStamp;
					pOutBuf->nFlags     = pInBuf->nFlags;
				}
			}
			ret = 0;
			goto Exit;
		}
		else if ( ret > 0  )
		{
			ret = 0;
			goto Exit;
		}

		pDecComp->bNeedKey = OMX_FALSE;
		pDecComp->bInitialized = OMX_TRUE;

		decIn.strmBuf = inData;
		decIn.strmSize = inSize;
		decIn.timeStamp = pInBuf->nTimeStamp;
		decIn.eos = 0;

		ret = NX_VidDecDecodeFrame( pDecComp->hVpuCodec, &decIn, &decOut );
	}
	else
	{
		decIn.strmBuf = inData;
		decIn.strmSize = inSize;
		decIn.timeStamp = pInBuf->nTimeStamp;
		decIn.eos = 0;
		ret = NX_VidDecDecodeFrame( pDecComp->hVpuCodec, &decIn, &decOut );
	}

	TRACE("decOut : outImgIdx(%d) decIdx(%d) readPos(%d), writePos(%d) \n", decOut.outImgIdx, decOut.outDecIdx, decOut.strmReadPos, decOut.strmWritePos );
	TRACE("Output Buffer : ColorFormat(0x%08x), NatvieBuffer(%d), Thumbnail(%d), MetaDataInBuffer(%d), ret(%d)\n",
			pDecComp->outputFormat.eColorFormat, pDecComp->bUseNativeBuffer, pDecComp->bEnableThumbNailMode, pDecComp->bMetaDataInBuffers, ret );

	if ( ret==VID_ERR_NONE && decOut.outImgIdx >= 0 && ( decOut.outImgIdx < NX_OMX_MAX_BUF ) )
	{
		if ( OMX_TRUE == pDecComp->bEnableThumbNailMode )
		{
			//	Thumbnail Mode
			NX_VID_MEMORY_INFO *pImg = &decOut.outImg;
			NX_PopQueue( pOutQueue, (void**)&pOutBuf );
			CopySurfaceToBufferYV12( (OMX_U8*)pImg->luVirAddr, (OMX_U8*)pImg->cbVirAddr, (OMX_U8*)pImg->crVirAddr,
				pOutBuf->pBuffer, pImg->luStride, pImg->cbStride, pDecComp->width, pDecComp->height );

			NX_VidDecClrDspFlag( pDecComp->hVpuCodec, NULL, decOut.outImgIdx );
			pOutBuf->nFilledLen = pDecComp->width * pDecComp->height * 3 / 2;
			if( 0 != PopVideoTimeStamp(pDecComp, &pOutBuf->nTimeStamp, &pOutBuf->nFlags )  )
			{
				pOutBuf->nTimeStamp = pInBuf->nTimeStamp;
				pOutBuf->nFlags     = pInBuf->nFlags;
			}
			DbgMsg("ThumbNail Mode : pOutBuf->nAllocLen = %ld, pOutBuf->nFilledLen = %ld\n", pOutBuf->nAllocLen, pOutBuf->nFilledLen );
			pDecComp->outFrameCount++;
			pDecComp->pCallbacks->FillBufferDone(pDecComp->hComp, pDecComp->hComp->pApplicationPrivate, pOutBuf);
		}
		else
		{
			int32_t OutIdx = ( pDecComp->bInterlaced == 0 ) ? ( decOut.outImgIdx ) : ( GetUsableBufferIdx(pDecComp) );
			pDecComp->isOutIdr = OMX_TRUE;
			pOutBuf = pDecComp->pOutputBuffers[OutIdx];

			if ( pDecComp->outBufferUseFlag[OutIdx] == 0 )
			{
				OMX_TICKS timestamp;
				OMX_U32 flag;
				PopVideoTimeStamp(pDecComp, &timestamp, &flag );
				NX_VidDecClrDspFlag( pDecComp->hVpuCodec, NULL, decOut.outImgIdx );
				ErrMsg("Unexpected Buffer Handling!!!! Goto Exit(%ld,%d)\n", pDecComp->curOutBuffers, decOut.outImgIdx);
				goto Exit;
			}
			pDecComp->outBufferValidFlag[OutIdx] = 1;
			pDecComp->outBufferUseFlag[OutIdx] = 0;
			pDecComp->curOutBuffers --;

			pOutBuf->nFilledLen = sizeof(struct private_handle_t);
			if ( 0 != PopVideoTimeStamp(pDecComp, &pOutBuf->nTimeStamp, &pOutBuf->nFlags )  )
			{
				pOutBuf->nTimeStamp = pInBuf->nTimeStamp;
				pOutBuf->nFlags     = pInBuf->nFlags;
			}
			TRACE("pOutBuf->nTimeStamp = %lld\n", pOutBuf->nTimeStamp/1000);

			DeInterlaceFrame( pDecComp, &decOut );
			pDecComp->outFrameCount++;
			pDecComp->pCallbacks->FillBufferDone(pDecComp->hComp, pDecComp->hComp->pApplicationPrivate, pOutBuf);
		}
	}
	else if( ret == VID_ERR_RUN )
	{
		// HEVC S/W Run Error : Need TimeStamp Pop
		OMX_TICKS timestamp;
		OMX_U32 flag;
		PopVideoTimeStamp(pDecComp, &timestamp, &flag );
		ret = 0;
	}

Exit:
	pInBuf->nFilledLen = 0;
	pDecComp->pCallbacks->EmptyBufferDone(pDecComp->hComp, pDecComp->hComp->pApplicationPrivate, pInBuf);

	return ret;
}
