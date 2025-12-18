package application.model;

import application.controller.CommandController;
import application.model.manager.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * 命令行控制相关
 */
public class CommandAppTools {

    private static byte[] writeBuf = new byte[Manager.BLOCK_SIZE];
    /**
     * writeBuf的有效输入长度
     */
    private static int validLengthToWrite;

    /**
     * 当前工作目录路径
     */
    private static String currentDirectory = "/";

    /**
     * 返回所有命令
     * 
     * @return 所有命令组成的字符串
     */
    private static String allCommand() {
        return "-----------------------\n"
                + "/h\n/cls\n/pwd\n/cd\n/ls\n"
                + "/create_file\n/open_file\n/read_file\n/write_file\n/close_file\n/delete_file\n/typefile\n/change\n"
                + "/md\n/dir\n/rd";
    }

    /**
     * 执行命令
     * 
     * @param controller 控制器
     * @param sec        字段
     */
    public static void executeCommand(CommandController controller, String[] sec) {
        String type = sec[0].substring(1).toLowerCase();
        String[] args = Arrays.copyOfRange(sec, 1, sec.length);
        if (type.equals("")) {
            controller.addOutput("未知命令，使用\"/h\"查看所有命令");
            controller.clearInput();
            return;
        }
        switch (type) {
            case "create_file": // 创建文件
                if (args.length != 2) {
                    controller.addOutput("命令格式: /create_file <文件绝对路径> <文件属性>");
                    break;
                }
                createFile(controller, args);
                break;

            case "open_file": // 打开文件
                if (args.length != 2) {
                    controller.addOutput("命令格式: /open_file <文件绝对路径> <操作类型(r|w|rw)>");
                    break;
                }
                openFile(controller, args);
                break;

            case "read_file": // 读文件
                if (args.length != 2) {
                    controller.addOutput("命令格式: /read_file <文件绝对路径> <读取长度>");
                    break;
                }
                readFile(controller, args);
                break;

            case "write_buffer": // 写缓冲
                if (args.length < 1) {
                    controller.addOutput("命令格式: /write_buffer <写入的内容>");
                    break;
                }
                // 将所有参数合并成一个字符串
                String[] mergedArgs = new String[1];
                mergedArgs[0] = String.join(" ", args);
                writeBuffer(controller, mergedArgs);
                break;

            case "write_file": // 写文件
                if (args.length != 1 && args.length != 2) {
                    // [] 表示可选
                    controller.addOutput("命令格式: /write_file <文件绝对路径> [写长度]");
                    break;
                }
                writeFile(controller, args);
                break;

            case "close_file": // 关闭文件
                if (args.length != 1) {
                    controller.addOutput("命令格式: /close_file <文件绝对路径>");
                    break;
                }
                closeFile(controller, args);
                break;

            case "delete_file": // 删除文件
                if (args.length != 1) {
                    controller.addOutput("命令格式: /delete_file <文件绝对路径>");
                    break;
                }
                deleteFile(controller, args);
                break;

            case "typefile": // 显示文件内容
                if (args.length != 1) {
                    controller.addOutput("命令格式: /typefile <文件绝对路径>");
                    break;
                }
                typeFile(controller, args);
                break;

            case "change": // 改变文件属性
                if (args.length != 2) {
                    controller.addOutput("命令格式: /change <文件绝对路径> <文件属性>");
                    break;
                }
                changeAttribute(controller, args);
                break;

            case "rename": // 改变文件属性
                if (args.length != 2) {
                    controller.addOutput("命令格式: /change <文件绝对路径> <新的文件名>");
                    break;
                }
                rename(controller, args);
                break;

            case "md": // 建立目录
                if (args.length != 1) {
                    controller.addOutput("命令格式: /md <目录绝对路径>");
                    break;
                }
                createDir(controller, args);
                break;

            case "dir": // 显示目录内容
                if (args.length != 1) {
                    controller.addOutput("命令格式: /dir <目录绝对路径>");
                    break;
                }
                showDir(controller, args);
                break;

            case "rd": // 删除空目录
                if (args.length != 1) {
                    controller.addOutput("命令格式: /rd <空目录绝对路径>");
                    break;
                }
                deleteDir(controller, args);
                break;

            case "cd": // 切换目录
                if (args.length != 1) {
                    controller.addOutput("命令格式: /cd <目录路径>");
                    break;
                }
                changeDirectory(controller, args);
                break;

            case "pwd": // 显示当前目录
                controller.addOutput("当前目录: " + currentDirectory);
                break;

            case "ls": // 列出当前目录内容
                listCurrentDirectory(controller);
                break;

            case "h": // 查看所有命令
                controller.addOutput("\n" + allCommand());
                break;
            case "cls": // 清屏
                controller.clearOutput();
                break;

            default:
                controller.addOutput("未知命令，使用\"/h\"查看所有命令");
        }
        controller.clearInput();
        // 修复：检查managerController和usageController是否为null
        // 它们只在对应的窗口被打开时才会初始化
        if (GUI.managerController != null) {
            GUI.managerController.recreateTree();
            GUI.managerController.update();
        }
        if (GUI.usageController != null) {
            GUI.usageController.update();
        }
    }

    /**
     * 建立文件
     * 
     * @param controller 命令行控制器
     * @param args       参数表(<文件绝对路径> <文件属性0-9>)
     */
    private static void createFile(CommandController controller, String[] args) {
        String absolutePath = args[0];
        byte attribute = (byte) Integer.parseInt(args[1]);

        String[] path = absolutePath.split("/");

        // 值为"文件"或"目录"
        String fileOrDir;

        /* 文件名和类型需合法 */
        String nameWithType = path[path.length - 1];
        String[] nwt = nameWithType.split("\\.");
        if (nwt.length != 1 && nwt.length != 2) {
            controller.addOutput("文件名不合法");
            return;
        }
        if (nwt.length == 1) { // 创建目录
            fileOrDir = "目录";
            if (nwt[0].length() > 3) {
                controller.addOutput("目录名不可超过3个字节");
                return;
            }
        } else { // 创建文件
            fileOrDir = "文件";
            if (nwt[0].length() > 3 || nwt[1].length() > 2) {
                controller.addOutput("文件名不可超过3个字节，类型名不可超过2个字节");
                return;
            }
        }

        /* 不能建立只读文件 */
        if ((attribute & 0x01) == 0x01) {
            controller.addOutput(String.format("不能建立只读%s", fileOrDir));
            return;
        }
        /* 依据文件路径找到父目录 */
        int startNum = locateParent(path);
        if (startNum == -1) {
            controller.addOutput("父目录不存在");
            return;
        }

        int result;
        if (nwt.length == 1) {
            result = Tools.createDir(nameWithType, attribute, startNum);
        } else {
            result = Tools.createFile(nameWithType, attribute, startNum);
        }
        if (result == 0) {
            controller.addOutput(String.format("目录下存在同名%s", fileOrDir));
            return;
        }
        if (result == -1) {
            controller.addOutput(String.format("磁盘已满，无法创建%s", fileOrDir));
            return;
        }
        if (result == -2) {
            controller.addOutput(String.format("%s名存在非法字符", fileOrDir));
            return;
        }
        if (result == 1) { // 创建成功
            controller.addOutput(String.format("%s创建成功", fileOrDir));
        }
    }

    /**
     * 判断文件是否存在，传递该文件在父目录的目录项，检查打开方式，确保不能以写方式打开只读文件
     * 
     * @param controller 命令行控制器
     * @param args       参数表(<文件绝对路径> <操作类型>)
     * @return 1 打开成功， 0 到达打开文件数量上限, 打开失败, -1 父目录不存在, -2 要打开的文件不存在, -3 不能以写方式打开只读文件
     */
    private static int openFile(CommandController controller, String[] args) {
        // fixme: args的参数：absolutePath要打开文件的绝对路径, operateType操作类型
        String absolutePath = args[0];
        String operateType = args[1];

        // 判断父目录是否存在，若存在，则得到父目录的起始盘块号
        String[] path = absolutePath.split("/");
        int parStartNum = locateParent(path);
        if (parStartNum == -1) {
            controller.addOutput("父目录不存在");
            return -1;
        }

        // 判断要打开的文件是否存在，若存在，则得到posOfEntry
        String fileName = path[path.length - 1];
        long posOfEntry = Tools.getPosOfEntry(fileName, parStartNum);

        if (posOfEntry == -1) { // 要打开的文件不存在
            controller.addOutput("要打开的文件不存在");
            return -2;
        }

        String[] fileInfo = Tools.getFileInfo(posOfEntry);

        // 不能以写方式打开只读文件
        byte attribute = (byte) Integer.parseInt(fileInfo[2]);
        if ((operateType.equals("w") || operateType.equals("rw")) && (attribute & 0x01) == 0x01) {
            controller.addOutput("不能以写方式打开只读文件");
            return -3;
        }

        int result = Tools.openFile(posOfEntry, operateType);
        if (result == 1) { // 创建成功
            controller.addOutput("文件打开成功");
            GUI.managerController.update();
            GUI.usageController.update();
        }
        return result;
    }

    /**
     * 查找已打开文件表中是否存在该文件；如果不存在，则打开后再读；然后检查是否是以读方式打开文件，如果是以写方式打开文件，则不允许读；
     * 
     * @param controller 命令行控制器
     * @param args       参数表(<文件绝对路径> <读取长度>)
     */
    private static void readFile(CommandController controller, String[] args) {
        String absolutePath = args[0];
        int readByteNum = Integer.parseInt(args[1]);

        if (readByteNum <= 0) {
            controller.addOutput("要读取的字节数不能小于等于0");
            return;
        }

        // 判断父目录是否存在
        String[] path = absolutePath.split("/");
        int parStartNum = locateParent(path);
        if (parStartNum == -1) {
            controller.addOutput("父目录不存在");
            return;
        }

        String nameWithType = path[path.length - 1];
        // 得到该文件的posOfEntry，又可判断该文件是否存在
        long posOfEntry = Tools.getPosOfEntry(nameWithType, parStartNum);
        if (posOfEntry == -1) {
            controller.addOutput("文件不存在");
            return;
        }

        // 查找已打开文件表中是否存在该文件, 若打开文件表中不存在，则打开该文件
        OFTLE oftle = OpenFile.find(posOfEntry);
        if (oftle == null) { // OpenFile表中不存在要读的文件, 即文件没有打开
            if (openFile(controller, new String[] { absolutePath, "rw" }) == 1) {
                oftle = OpenFile.find(posOfEntry);
            } else {
                controller.addOutput("打开文件失败");
                return;
            }
        }
        // 得到要读的文件的oftle

        // 如果是以写方式打开文件，则不允许读
        if (oftle.getOperateType() == 1) {
            return;
        }

        // 最后从已打开文件表中读出读指针，从这个位置上读出所需要长度
        int diskNum = oftle.getRead().getDiskNum();
        try (RandomAccessFile raf = new RandomAccessFile(Manager.FILE_NAME, "r")) {
            while (readByteNum > 0) {
                int readNum;
                readNum = Math.min(readByteNum, Manager.BLOCK_SIZE);
                int indexOfEOF = readNum; // 因为readBuf的大小为readNum，所以初始化为readNum
                raf.seek((long) diskNum * Manager.BLOCK_SIZE);
                byte[] readBuf = new byte[readNum];
                for (int i = 0; i < readNum; i++) {
                    readBuf[i] = raf.readByte();
                    if (readBuf[i] == Manager.EOF) { // 若所需长度没有读完已经遇到文件结束符
                        indexOfEOF = i;
                        break;
                    }
                }
                // 若indexOfEOF == readNum，则为原数组 或 若indexOfEOF ==
                // EOF在readBuf的下标中，则为[0,indexOfEOF-1]数组
                controller.addSuccessiveOutput(new String(readBuf).substring(0, indexOfEOF));

                // 读完最后一个盘块 或 读到了结束符
                if (Manager.FAT[diskNum] == -1 || indexOfEOF != readNum) {
                    break;
                }
                diskNum = Manager.FAT[diskNum];
                readByteNum -= Manager.BLOCK_SIZE; // 完全读完一个盘块
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将写入的内容存入writeBuf中, writeBuf上限为一个盘块大小，要写入的内容超出的部分不存入writeBuf
     * 
     * @param args <写入的内容>
     */
    private static void writeBuffer(CommandController controller, String[] args) {
        byte[] contentToWrite = args[0].getBytes();
        validLengthToWrite = Math.min(contentToWrite.length, writeBuf.length);
        System.arraycopy(contentToWrite, 0, writeBuf, 0, validLengthToWrite);
    }

    /**
     * 是查找已打开文件表中是否存在该文件，如果不存在，则打开后再写；
     * 如果存在，如果是读方式打开文件，不能写
     * 最后从已打开文件表中读出写指针，从这个位置上写入缓冲中的数据
     * 写入完成后，最后清空缓存
     *
     * 写文件有两种情况，一种情况是建立文件后的写入， 另一种情况是文件打开后的写入，只要求完成从文件末尾向后追加的功能
     *
     * @param controller
     * @param args       <文件绝对路径> [写长度]
     */
    private static void writeFile(CommandController controller, String[] args) {
        String absolutePath = args[0];
        int lengthToWrite;
        int argsLengthToWrite;
        if (args.length == 2 && (argsLengthToWrite = Integer.parseInt(args[1])) >= 0) {
            lengthToWrite = Math.min(argsLengthToWrite, validLengthToWrite);
        } else {
            lengthToWrite = validLengthToWrite;
        }

        /* 依据文件路径找到父目录 */
        String[] path = absolutePath.split("/");
        int parStartNum = locateParent(path);
        if (parStartNum == -1) {
            controller.addOutput("父目录不存在");
            return;
        }

        // 得到该文件的posOfEntry，又可判断该文件是否存在
        String nameWithType = path[path.length - 1];
        long posOfEntry = Tools.getPosOfEntry(nameWithType, parStartNum);
        if (posOfEntry == -1) {
            controller.addOutput("文件不存在");
            return;
        }

        // 查找已打开文件表中是否存在该文件, 若打开文件表中不存在，则打开该文件
        OFTLE oftle = OpenFile.find(posOfEntry);
        if (oftle == null) { // OpenFile表中不存在要读的文件
            if (openFile(controller, new String[] { absolutePath, "rw" }) == 1) {
                oftle = OpenFile.find(posOfEntry);
            } else {
                controller.addOutput("打开文件失败");
                return;
            }
        }

        // 如果是读方式打开文件，不能写
        if (oftle.getOperateType() == 0) {
            return;
        }

        int diskNum = oftle.getWrite().getDiskNum();
        int byteNum = oftle.getWrite().getByteNum();

        try (RandomAccessFile raf = new RandomAccessFile(Manager.FILE_NAME, "rw")) {
            int indexOfWriteBuf = 0;
            raf.seek((long) diskNum * Manager.BLOCK_SIZE + byteNum);
            while (indexOfWriteBuf < lengthToWrite) {
                raf.writeByte(writeBuf[indexOfWriteBuf++]);
                byteNum++;
                if (byteNum == Manager.BLOCK_SIZE) { // 写完一个磁盘块
                    if (Manager.FAT[diskNum] == -1) { // 是最后一个磁盘块，则分配一个空闲的磁盘块
                        int idleDiskNum;
                        if ((idleDiskNum = Tools.getIdleDiskNum()) == -1) {
                            controller.addOutput("没有空闲磁盘块， 无法继续写入");
                            return;
                        }
                        Tools.updateFAT(raf, diskNum, idleDiskNum); // 分配新的一个磁盘块，更新FAT
                        Tools.updateFAT(raf, idleDiskNum, -1);
                    }
                    diskNum = Manager.FAT[diskNum]; // 写下一个磁盘块
                    byteNum = 0;
                    raf.seek((long) diskNum * Manager.BLOCK_SIZE);
                }
            }
            raf.writeByte(Manager.EOF); // 追加文件结束符
            // fixme: 若刚好写满一个盘块，要为文件结束符分配一个新盘块来存放
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 写入完成后，最后清空缓存
        Arrays.fill(writeBuf, (byte) 0);
    }

    /**
     * 关闭文件
     * 
     * @param args <文件绝对路径>
     */
    private static void closeFile(CommandController controller, String[] args) {
        String absolutePath = args[0];

        String[] path = absolutePath.split("/");
        // 判断父目录是否存在
        int parStartNum = locateParent(path);
        if (parStartNum == -1) {
            controller.addOutput("父目录不存在");
            return;
        }
        // 得到该文件的posOfEntry，又可判断该文件是否存在
        String nameWithType = path[path.length - 1];
        long posOfEntry = Tools.getPosOfEntry(nameWithType, parStartNum);
        if (posOfEntry == -1) {
            controller.addOutput("文件不存在");
            return;
        }
        Tools.closeFile(posOfEntry);
        controller.addOutput("文件关闭成功");
    }

    /**
     * 删除文件
     *
     * @param controller
     * @param args       <文件绝对路径>
     */
    private static void deleteFile(CommandController controller, String[] args) {
        String absolutePath = args[0];

        // 检查文件的父目录是否存在
        String[] path = absolutePath.split("/");
        int parStartNum = locateParent(path);
        if (parStartNum == -1) {
            controller.addOutput("父目录不存在");
            return;
        }

        // 检查文件是否存在
        String nameWithType = path[path.length - 1];
        long posOfEntry = Tools.getPosOfEntry(nameWithType, parStartNum);
        if (posOfEntry == -1) {
            controller.addOutput("要删除的文件不存在");
            return;
        }

        int result = Tools.deleteFile(posOfEntry);
        if (result == 1) {
            controller.addOutput("删除成功");
        } else {
            controller.addOutput("待删除文件正在打开，不能删除");
        }
    }

    /**
     * @param controller
     * @param args       <文件绝对路径>
     */
    private static void typeFile(CommandController controller, String[] args) {
        String absolutePath = args[0];

        // 检查文件的父目录是否存在
        String[] path = absolutePath.split("/");
        int parStartNum = locateParent(path);
        if (parStartNum == -1) {
            controller.addOutput("父目录不存在");
            return;
        }

        // 检查文件是否存在
        String nameWithType = path[path.length - 1];
        long posOfEntry = Tools.getPosOfEntry(nameWithType, parStartNum);
        if (posOfEntry == -1) {
            controller.addOutput("要删除的文件不存在");
            return;
        }

        String[] info = Tools.typeFile(posOfEntry);

        controller.addOutput("名称：" + nameWithType);
        controller.addOutput("属性(二进制)：" + Integer.toBinaryString(Integer.parseInt(info[2])));
        controller.addOutput("总字节数：" + info[5]);

        // 显示文件内容
        controller.addOutput("------- 文件内容 -------");
        int byteLength = Integer.parseInt(info[5]);
        if (byteLength > 0) {
            int startNum = Integer.parseInt(info[3]);
            int diskNum = startNum;
            try (RandomAccessFile raf = new RandomAccessFile(Manager.FILE_NAME, "r")) {
                int remain = byteLength;
                while (remain > 0) {
                    raf.seek((long) diskNum * Manager.BLOCK_SIZE);
                    int readSize = Math.min(remain, Manager.BLOCK_SIZE);
                    byte[] buf = new byte[readSize];
                    for (int i = 0; i < readSize; i++) {
                        buf[i] = raf.readByte();
                        // 遇到 EOF 提前结束 (理论上 byteLength 应该准确，但为了安全)
                        if (buf[i] == Manager.EOF) {
                            readSize = i;
                            break;
                        }
                    }
                    controller.addSuccessiveOutput(new String(buf, 0, readSize));

                    remain -= Manager.BLOCK_SIZE;
                    if (remain > 0) {
                        diskNum = Manager.FAT[diskNum];
                        if (diskNum == -1)
                            break; // 异常：FAT链断裂
                    }
                }
                controller.addOutput(""); // 换行
            } catch (Exception e) {
                e.printStackTrace();
                controller.addOutput("(读取错误)");
            }
        } else {
            controller.addOutput("(空文件)");
        }
    }

    /**
     * 首先查找该文件，如果不存在，结束；如果存在，
     * 检查文件是否打开，打开不能改变属性；没有打开，根据要求改变目录项中属性值。
     * 
     * @param controller
     * @param args       <文件绝对路径> <文件属性>
     */
    private static void changeAttribute(CommandController controller, String[] args) {
        String absolutePath = args[0];
        byte[] bytes = args[1].getBytes();
        byte newAttribute = bytes[0];

        // 检查文件的父目录是否存在
        String[] path = absolutePath.split("/");
        int parStartNum = locateParent(path);
        if (parStartNum == -1) {
            controller.addOutput("父目录不存在");
            return;
        }

        // 检查文件是否存在
        String nameWithType = path[path.length - 1];
        long posOfEntry = Tools.getPosOfEntry(nameWithType, parStartNum);
        if (posOfEntry == -1) {
            controller.addOutput("要删除的文件不存在");
            return;
        }

        int result = Tools.changeAttribute(posOfEntry, newAttribute);
        if (result == 1) {
            controller.addOutput("修改属性成功");
        } else {
            controller.addOutput("文件打开中，不能修改");
        }
    }

    /**
     *
     * @param controller
     * @param args       <文件或目录的绝对路径> <新的文件名（包含扩展名）或目录名>
     */
    public static void rename(CommandController controller, String[] args) {
        String absolutePath = args[0];
        String newNameWithType = args[1];

        // 检查文件的父目录是否存在
        String[] path = absolutePath.split("/");
        int parStartNum = locateParent(path);
        if (parStartNum == -1) {
            controller.addOutput("父目录不存在");
            return;
        }

        // 检查文件是否存在
        String nameWithType = path[path.length - 1];
        long posOfEntry = Tools.getPosOfEntry(nameWithType, parStartNum);
        if (posOfEntry == -1) {
            controller.addOutput("要重命名的文件不存在");
            return;
        }
        int result = Tools.rename(posOfEntry, newNameWithType);

        if (result == 1) {
            controller.addOutput("文件重命名成功");
        } else if (result == 0) {
            controller.addOutput("文件打开中，不能重命名");
        } else {
            controller.addOutput("输入的文件名不合法");
        }
    }

    /**
     * 建立目录
     * 
     * @param args <目录绝对路径>
     */
    private static void createDir(CommandController controller, String[] args) {
        // 修复：/md命令只传递路径参数，需要自动添加属性参数
        // 目录属性必须包含目录标志位：0x08（二进制：0b00001000）
        // 普通目录：0x0C（二进制：0b00001100 = 目录0x08 + 普通文件0x04）= 十进制12
        String[] argsWithAttribute = new String[2];
        argsWithAttribute[0] = args[0]; // 路径
        argsWithAttribute[1] = "12"; // 属性：普通目录（0x0C = 12）
        createFile(controller, argsWithAttribute);
    }

    /**
     * 显示目录内容
     * 
     * @param args <目录绝对路径>
     */
    private static void showDir(CommandController controller, String[] args) {
        String absolutePath = args[0];

        // 特殊处理：根目录
        if (absolutePath.equals("/")) {
            controller.addOutput("名称\t属性（二进制）\t总字节数");
            int parDirDiskNum = 2; // 根目录的起始盘块号是2

            while (true) {
                long posOfFileEntry = (long) parDirDiskNum * Manager.BLOCK_SIZE;

                for (int i = 0; i < Manager.DIR_ENTRY_NUM; i++) {
                    String[] fileInfo = Tools.getFileInfo(posOfFileEntry);

                    String name = fileInfo[0].trim(); // 去除空白字符
                    String type = fileInfo[1].trim();
                    byte attribute = (byte) Integer.parseInt(fileInfo[2]);

                    // 修复：跳过空目录项（名称以$开头或为空）
                    if (name.isEmpty() || name.startsWith("$") || name.charAt(0) == Manager.IDLE_ENTRY) {
                        posOfFileEntry += Manager.DIR_ENTRY_NUM;
                        continue;
                    }

                    if (((attribute) & 0x08) != 0x08) { // 该子文件是文件
                        name += "." + type;
                    }

                    controller.addSuccessiveOutput("名称：" + name + "\t");
                    controller.addSuccessiveOutput("属性(二进制)：" + Integer.toBinaryString(attribute) + "\t");
                    controller.addOutput("总字节数：" + fileInfo[5]);

                    posOfFileEntry += Manager.DIR_ENTRY_NUM;
                }
                if (Manager.FAT[parDirDiskNum] == -1) {
                    break;
                } else {
                    parDirDiskNum = Manager.FAT[parDirDiskNum];
                }
            }
            return;
        }

        // 判断父目录是否存在
        String[] path = absolutePath.split("/");
        int parStartNum = locateParent(path);
        if (parStartNum == -1) {
            controller.addOutput("父目录不存在");
            return;
        }

        // 检查目录是否存在
        String dirName = path[path.length - 1];
        long posOfEntry = -1;
        int parDirDiskNum = parStartNum;

        // 修复：目录没有扩展名，需要直接匹配目录名
        try (RandomAccessFile raf = new RandomAccessFile(Manager.FILE_NAME, "rw")) {
            byte[] entry = new byte[Manager.DIR_ENTRY_NUM];
            // 在父目录中查找该目录是否存在
            while (true) {
                raf.seek((long) parDirDiskNum * Manager.BLOCK_SIZE);
                for (int i = 0; i < Manager.DIR_ENTRY_NUM; i++) {
                    long currentPos = raf.getFilePointer();
                    raf.readFully(entry);
                    // 读取目录名（3个字节）
                    String name = new String(new byte[] { entry[0], entry[1], entry[2] }).trim();
                    byte attribute = entry[5];

                    // 检查是否为目录且名称匹配
                    if ((attribute & 0x08) == 0x08 && name.equals(dirName)) {
                        posOfEntry = currentPos;
                        break;
                    }
                }

                if (posOfEntry != -1) {
                    break; // 找到了
                }

                // 遍历完最后一个盘块还没找到目标目录
                if (Manager.FAT[parDirDiskNum] == -1) {
                    controller.addOutput("目录不存在");
                    return;
                } else {
                    parDirDiskNum = Manager.FAT[parDirDiskNum];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            controller.addOutput("读取目录时出错");
            return;
        }

        // 显示各个目录项
        controller.addOutput("名称\t属性（二进制）\t总字节数");

        String[] dirInfo = Tools.getFileInfo(posOfEntry);
        parDirDiskNum = Integer.parseInt(dirInfo[3]); // 该目录的起始盘块号

        while (true) {
            long posOfFileEntry = (long) parDirDiskNum * Manager.BLOCK_SIZE; // 某个盘块的第一个目录项的位置

            for (int i = 0; i < Manager.DIR_ENTRY_NUM; i++) { // 某一个盘块内
                String[] fileInfo = Tools.getFileInfo(posOfFileEntry); // 读这个子文件的目录项的信息

                String name = fileInfo[0].trim(); // 去除空白字符
                String type = fileInfo[1].trim();
                byte attribute = (byte) Integer.parseInt(fileInfo[2]);

                // 修复：跳过空目录项
                if (name.isEmpty() || name.startsWith("$") || name.charAt(0) == Manager.IDLE_ENTRY) {
                    posOfFileEntry += Manager.DIR_ENTRY_NUM;
                    continue;
                }

                if (((attribute) & 0x08) != 0x08) { // 该子文件是文件
                    name += "." + type;
                }

                controller.addSuccessiveOutput("名称：" + name + "\t");
                controller.addSuccessiveOutput("属性(二进制)：" + Integer.toBinaryString(attribute) + "\t");
                controller.addOutput("总字节数：" + fileInfo[5]); // 最后有个回车

                posOfFileEntry += Manager.DIR_ENTRY_NUM; // 该盘块内下一个目录项的位置
            }
            if (Manager.FAT[parDirDiskNum] == -1) { // 读完最后一个盘块
                break;
            } else { // 下一个盘块
                parDirDiskNum = Manager.FAT[parDirDiskNum];
            }
        }
    }

    /**
     * 删除指定目录
     * 
     * @param controller
     * @param args       <目录绝对路径>
     */
    private static void deleteDir(CommandController controller, String[] args) {
        String absolutePath = args[0];

        // 判断父目录是否存在
        String[] path = absolutePath.split("/");
        int parStartNum = locateParent(path);
        if (parStartNum == -1) {
            controller.addOutput("父目录不存在");
            return;
        }

        // 检查该目录是否存在
        String nameWithType = path[path.length - 1];
        long posOfEntry = Tools.getPosOfEntry(nameWithType, parStartNum);
        if (posOfEntry == -1) {
            controller.addOutput("目录不存在");
            return;
        }

        int result = Tools.deleteDir(posOfEntry);
        if (result == 1) {
            controller.addOutput("目录删除成功");
        } else {
            controller.addOutput("根目录不能删除");
        }
    }

    /**
     * 定位直接父目录, 绝对路径从前往后找，查看每个父目录是否存在
     * 
     * @param path 文件绝对路径中的字段
     * @return 起始盘块号，或-1（不存在）
     */
    public static int locateParent(String[] path) {
        int index = 2; // FAT指针从根目录开始
        try (RandomAccessFile raf = new RandomAccessFile(Manager.FILE_NAME, "rw")) {
            // 修复：从1开始，跳过split("/")产生的空字符串（当路径以/开头时）
            // 当path为["/tmp"]时，split结果为["", "tmp"]，需要从索引1开始
            for (int i = 1; i < path.length - 1; i++) {
                boolean isFound = false; // 是否找到当前目录段
                OUTER: while (index > 0) {
                    for (int j = 0; j < Manager.DIR_ENTRY_NUM; j++) { // 遍历盘块匹配目录名
                        // 跳到下个数据段
                        raf.seek((long) index * Manager.BLOCK_SIZE + j * 8);

                        // 取文件名转为字符串
                        byte[] bytes = { raf.readByte(), raf.readByte(), raf.readByte() };
                        String name = new String(bytes);
                        if (name.equals(path[i])) {
                            // 文件名匹配，判断是否为目录文件
                            System.out.println(name);
                            raf.skipBytes(2);
                            byte attribute = raf.readByte();
                            if ((attribute & 0x08) == 0x08) { // 找到目录文件
                                isFound = true;
                                index = raf.readByte(); // 指向此目录文件起始盘块号

                                if (i == path.length - 2) { // 是直接父目录文件，返回起始盘块号
                                    return index;
                                }

                                break OUTER; // 不是直接父目录的目录文件，离开while循环，进行下一段匹配
                            }
                        }
                    }
                    index = Manager.FAT[index]; // 往下一盘块
                }
                if (!isFound) { // 未找到当前目录段
                    return -1;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 2;
    }

    /**
     * 切换当前工作目录
     * 
     * @param controller 命令行控制器
     * @param args       参数（目录路径）
     */
    private static void changeDirectory(CommandController controller, String[] args) {
        String targetPath = args[0];

        // 特殊处理：当前目录 (cd .)
        if (targetPath.equals(".")) {
            controller.addOutput("当前目录: " + currentDirectory);
            return;
        }

        // 特殊处理：上一级目录 (cd ..)
        if (targetPath.equals("..")) {
            if (currentDirectory.equals("/")) {
                controller.addOutput("已在根目录，无法返回上一级");
                controller.addOutput("当前目录: " + currentDirectory);
                return;
            }

            // 解析当前目录，去掉最后一级
            String[] pathParts = currentDirectory.split("/");
            if (pathParts.length <= 2) {
                // 如果只有两个部分（["", "dirname"]），返回根目录
                currentDirectory = "/";
            } else {
                // 重新构建路径，去掉最后一级
                StringBuilder newPath = new StringBuilder();
                for (int i = 0; i < pathParts.length - 1; i++) {
                    if (i > 0) {
                        newPath.append("/");
                    }
                    if (!pathParts[i].isEmpty()) {
                        newPath.append(pathParts[i]);
                    }
                }
                currentDirectory = newPath.toString();
                if (currentDirectory.isEmpty()) {
                    currentDirectory = "/";
                }
            }
            controller.addOutput("当前目录: " + currentDirectory);
            return;
        }

        // 特殊处理：切换到根目录
        if (targetPath.equals("/")) {
            currentDirectory = "/";
            controller.addOutput("当前目录: " + currentDirectory);
            return;
        }

        // 验证目录是否存在
        String[] path = targetPath.split("/");
        int parStartNum = locateParent(path);
        if (parStartNum == -1) {
            controller.addOutput("目录不存在");
            return;
        }

        // 检查目标是否为目录
        String dirName = path[path.length - 1];
        long posOfEntry = -1;
        int parDirDiskNum = parStartNum;

        try (RandomAccessFile raf = new RandomAccessFile(Manager.FILE_NAME, "rw")) {
            byte[] entry = new byte[Manager.DIR_ENTRY_NUM];
            // 在父目录中查找该目录是否存在
            while (true) {
                raf.seek((long) parDirDiskNum * Manager.BLOCK_SIZE);
                for (int i = 0; i < Manager.DIR_ENTRY_NUM; i++) {
                    long currentPos = raf.getFilePointer();
                    raf.readFully(entry);
                    String name = new String(new byte[] { entry[0], entry[1], entry[2] }).trim();
                    byte attribute = entry[5];

                    // 检查是否为目录且名称匹配
                    if ((attribute & 0x08) == 0x08 && name.equals(dirName)) {
                        posOfEntry = currentPos;
                        break;
                    }
                }

                if (posOfEntry != -1) {
                    break; // 找到了
                }

                // 遍历完最后一个盘块还没找到目标目录
                if (Manager.FAT[parDirDiskNum] == -1) {
                    controller.addOutput("目录不存在或不是一个目录");
                    return;
                } else {
                    parDirDiskNum = Manager.FAT[parDirDiskNum];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            controller.addOutput("切换目录时出错");
            return;
        }

        // 成功找到目录，更新当前目录
        currentDirectory = targetPath;
        controller.addOutput("当前目录: " + currentDirectory);
    }

    /**
     * 列出当前工作目录的内容
     * 
     * @param controller 命令行控制器
     */
    private static void listCurrentDirectory(CommandController controller) {
        // 复用showDir的逻辑，传入当前目录
        showDir(controller, new String[] { currentDirectory });
    }

}
