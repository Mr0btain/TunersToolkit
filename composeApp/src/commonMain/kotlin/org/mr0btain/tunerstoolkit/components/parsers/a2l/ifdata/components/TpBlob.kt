package org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.components
import org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.components.tpblob.TPBBlobVersion
import org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.components.tpblob.TPBByteOrder
import org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.components.tpblob.TPBCANMsgIdReceive
import org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.components.tpblob.TPBCANMsgIdSend
import org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.components.tpblob.TPBCCPVersion
import org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.components.tpblob.TPBStationAddress
import org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.components.tpblob.TpBlobItems

data class TpBlob(
    val items: Map<String, TpBlobItems>
): IfDataSubItems

fun tpBlobParser(input: List<String>): Map<String, TpBlobItems> {
    val topBlobItemsList = mutableMapOf<String, TpBlobItems>()
    input.forEach { line ->
        when {
            line.startsWith("/* CCP version       */") -> {
                topBlobItemsList["CCPVersion"] = TPBCCPVersion(
                    line.replace("/* CCP version       */", "").trim()
                )
            }
            line.startsWith("/* Blob version      */") -> {
                topBlobItemsList["BlobVersion"] = TPBBlobVersion(
                    line.replace("/* Blob version      */", "").trim()
                )
            }
            line.startsWith("/* CAN msg ID - send */") -> {
                topBlobItemsList["CANMsgIdSend"] = TPBCANMsgIdSend(
                    line.replace("/* CAN msg ID - send */", "").trim()
                )
            }
            line.startsWith("/* CAN msg ID - recv */") -> {
                topBlobItemsList["CANMsgIdReceive"] = TPBCANMsgIdReceive(
                    line.replace("/* CAN msg ID - recv */", "").trim()
                )
            }
            line.startsWith("/* station address   */") -> {
                topBlobItemsList["StationAddress"] = TPBStationAddress(
                    line.replace("/* station address   */", "").trim()
                )
            }
            line.startsWith("/* byte order        */") -> {
                topBlobItemsList["ByteOrder"] = TPBByteOrder(
                    line.replace("/* byte order        */", "").trim()
                )
            }
            line.startsWith("/begin CAN_PARAM") -> {

            }
        }
    }
    return topBlobItemsList
}

    /*

    /begin TP_BLOB
       /* CCP version       */ 0x201
    /* Blob version      */ 0x202
    /* CAN msg ID - send */ 0x7C3
    /* CAN msg ID - recv */ 0x7C4
    /* station address   */ 0x12ED
    /* byte order        */ 1
    /begin CAN_PARAM
        /* crystal frequency */ 0x1F4
     /* timing register 0 */ 0x41
     /* timing register 1 */ 0x25
    /end CAN_PARAM
    DAQ_MODE    BURST
    CONSISTENCY ODT
    ADDRESS_EXTENSION ODT
       /begin CHECKSUM_PARAM
       /* algorithm */ 0xC001
        /* limit     */ 0xFFFFFFFF
        CHECKSUM_CALCULATION ACTIVE_PAGE
    /end CHECKSUM_PARAM
    /begin DEFINED_PAGES
        1
     "reference page"
     /* address ext. */ 0x1
     /* base address */ 0x5C2000
     /* page length  */ 0x2DFFF
     ROM
    /end DEFINED_PAGES
    OPTIONAL_CMD 0xC  /* SELECT_CAL_PAGE */
    OPTIONAL_CMD 0xD  /* BUILD_CHKSUM */
    OPTIONAL_CMD 0xE  /* MOVE */
    OPTIONAL_CMD 0x11  /* GET_ACTIVE_CAL_PAGE */
    OPTIONAL_CMD 0x19  /* SET_S_STATUS */
    OPTIONAL_CMD 0xD  /* GET_S_STATUS */
    OPTIONAL_CMD 0x12  /* GET_SEED */
    OPTIONAL_CMD 0x13  /* UNLOCK */
   /end TP_BLOB

    block "TP_BLOB" struct {
      uint;      /* Blob version, High byte: version   */
                 /*  Low byte : sub version (decimal.) */
                 /* current version 0x0200             */
      enum {     /* KWP2000 version                    */
         "VDA_1996" = 1
      };
      enum {     /* byte order in measurements and characteristics  */
         "MSB_FIRST" = 1,
         "MSB_LAST"  = 2
      };
      taggedstruct {
         block "K_LINE" struct {
            enum {    /* KW INI: stimulation mode of ECU: WuP or 5 baud */
               "WuP"               = 1,
               "Stimulation_5Baud" = 2
            };
            uint;       /* ECU address on K-line                        */
            uint;       /* tester address on K-Line                     */
         };
      };
      taggedstruct {
         block "CAN" struct {
            ulong;              /* Baudrate [Hz]                          */
            char;               /* Sample point [% complete bit time]     */
            char;               /* 1 or 3 samples per bit                 */
            char;               /* BTL_CYCLES [slots/bit time]            */
            char;               /* SJW length synchr. segm. in BTL_CYCLES */
            char;               /* SYNC_EDGE                              */
            taggedstruct {
               block "ADDRESS" struct {
                  ulong;        /* CAN_ID ECU(or gateway)->INCA           */
                  ulong;        /* CAN_ID INCA->ECU(or gateway)           */
                  taggedstruct {
                     "TGT_ECU"  uint;   /* Extended/remote address ECU    */
                     "TGT_INCA" uint;   /* Extended address INCA          */
                  };
               };
            };
            taggedstruct {
               "NETWORK_LIMITS" struct {
                  char;        /* WFT_MAX (max. waitframes)               */
                  uint;        /* XDL_MAX (max. datalength)               */
               };
            };
            taggedstruct {
               "START_STOP" uint;   /* Start_stop FREE_RUNNING :          */
                                    /* routine number                     */
            };
         };
      };
      taggedstruct {
         "DATA_ACCESS" struct {
            ulong;        /* ECU address type qualifier for Flash page    */
            ulong;        /* ECU address type qualifier for RAM page      */
            uint;         /* 1: Read access on ECU flash data             */
            uint;         /* 1: Verify access on ECU flash code           */
            uint;         /* 1: Read access on ECU flash code             */
            uint;         /* 1: Upload and download only on active page   */
         };
         block "CHECKSUM" struct {
            ulong;        /* check sum type qualifier (see table)         */
            uint;         /* 1: check sum calculation only on active page */
            uint;         /* No of local routine for check sum            */
            enum {        /* result of checksum routine delivered by      */
               "RequestRoutineResults" = 0,
               "StartRoutine"          = 1,
               "CodedResult"           = 2
            };
            taggedstruct {
               "RNC_RESULT" ( uchar )*; /* only at 'RequestRoutineResults': */
                                        /* result codes, that sign 'calculation in progress' */
            };
         };
         block "COPY" struct {
            enum {       /* ECU mode for copying Flash into RAM           */
               "RAM_InitByECU"   = 0x10,
               "RAM_InitByTool"  = 0x20
            };
            uint;               /* diagnostic mode for copy Flash  to RAM */
            taggedstruct {
               "COPY_PARA" ( uchar )*;/* list of add. Param for StartDiag */
            };
         };
         block "FLASH" struct {
            enum {                 /* ECU mode for copying RAM into Flash */
               "NOFLASHBACK"     = 0x00,
               "AUTOFLASHBACK"   = 0x40,
               "TOOLFLASHBACK"   = 0x80
            };
            uint;        /* No of local routine for copy RAM into flash   */
            enum {       /* result of copy routine delivered by           */
               "RequestRoutineResults" = 0,
               "StartRoutine"          = 1,
               "CodedResult"           = 2
            };
            taggedstruct {
               "COPY_FRAME" ( uchar )*; /* list of frames to be copied    */
               "RNC_RESULT" ( uchar )*; /* only at'RequestRoutineResults':*/
                                        /* result codes, that sign 'copy in progress' */
            };
         };
         (block "DIAG_BAUD" struct {    /* Baudrate switch:               */
            ulong;                      /* BaudRate                       */
            uint;                       /* DiagMode                       */
            taggedstruct {
               "BD_PARA" ( uchar )*;    /* bytes for ECU baudrate switch  */
            };
         }
         )*;
         block "TIME_DEF" struct {      /* Timing parameter:              */
            taggedstruct {
               ("KWP_TIMING" struct {
                  uint;                 /* p1        [ms]                */
                  uint;                 /* p2Min     [ms]                */
                  uint;                 /* p2MAX     [ms]                */
                  uint;                 /* p3Min     [ms]                */
                  uint;                 /* p3MAX     [ms]                */
                  uint;                 /* p4        [ms]                */
               }
               )*;
            };
            taggedstruct {
               ("USDTP_TIMING" struct {
                  uint;                 /* As [ms]                       */
                  uint;                 /* Bs [ms]                       */
                  uint;                 /* Cr [ms]                       */
               }
               )*;
            };
         };
         ( "SECURITY_ACCESS" struct { /* Parameter for Seed&Key:         */
            uint;   /* Access mode for KWP2000 service 27h               */
            uint;   /* method of seed&key calculation (see table)        */
            uint;   /* delaytime in sec from stimulation to seed request */
         }
         )*;
         block "PAGE_SWITCH" struct {
            enum {                         /* ECU mode for switching pages         */
               "ESCAPE_CODE"   = 0x80,
               "LOCAL_ROUTINE" = 0x31
            };
            taggedstruct {
               "ESCAPE_CODE_PARA_SET"  ( uchar )*;
               "ESCAPE_CODE_PARA_GET"  ( uchar )*;
               block "ROUTINE_PARA" struct {
                  uint;               /* No of local routine for page switching    */
                  enum {              /* result of switching routine delivered by  */
                     "RequestRoutineResults" = 0,
                     "StartRoutine"          = 1,
                     "CodedResult"           = 2
                  };
                  taggedstruct {
                     "RNC_RESULT" ( uchar )*;    /*only at'RequestRoutineResults': */
                                                 /* result codes, that sign 'switching in progress' */
                  };
               };
               "PAGE_CODE" ( uchar )*;           /* codes for RP and WP            */
            };
         };
      };
   };
     */