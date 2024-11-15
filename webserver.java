import java.io.* ;
import java.net.* ;
import java.util.* ;


public final class webserver
{
	public static void main(String argv[]) throws Exception
	{
		//設定port值
	    int port = 6789;
        // 建立一個物件來處理 HTTP 請求訊息。
        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {            
            //等待客戶端連接
            Socket clientSocket = serverSocket.accept();
            //使用clientSocket創建HttpRequest物件
            HttpRequest request = new HttpRequest(clientSocket);
            Thread thread = new Thread(request);
            thread.start();
        }
	    
	}
}

final class HttpRequest implements Runnable
{
	Socket socket;
    private static final Map<String, String> USER_DB = new HashMap<>();
	// Constructor
	public HttpRequest(Socket socket) throws Exception 
	{
		this.socket = socket;
	}

	//實作Runnable介面的run()方法
	public void run()
	{
		try {
            extractFilename();
        } catch (Exception e) {
            System.out.println(e);
        }
	}

    public void extractFilename() throws Exception
    {
        {
            //獲取輸入和輸出流
            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            DataOutputStream os = new DataOutputStream(socket.getOutputStream());
            //從請求行提取檔案名稱
            String requestLine = br.readLine();
            System.out.println("Request: " + requestLine);
            StringTokenizer tokens = new StringTokenizer(requestLine);
            //跳過應為"GET"的方法。
            tokens.nextToken();  
            String fileName = tokens.nextToken();
            String textName=fileName;
            //將密碼輸入時，刪除前面的斜線
            textName=textName.substring(1);
            //在檔案請求前加上"."，以確保檔案在目前目錄中。
            fileName = "." + fileName;
            FileInputStream fis = null;
            boolean fileExists = true;
            boolean textExists = true;
            String statusLine = null;
            String contentTypeLine = null;
            String entityBody = null;
            

            //定義CRLF為換行符號
            final String CRLF = "\r\n"; 
            //顯示登入server之瀏覽器之訊息
            {
                if(requestLine!=null)
                {
                    System.out.println();
                    System.out.println(requestLine);
                    String headerLine = null;
                    while ((headerLine = br.readLine()).length() != 0) 
                    {
	                    System.out.println(headerLine); 
                    }
                }
                else
                {
                    System.out.println("NO new URL"); 
                }
            }
            //嘗試開啟請求的檔案，檢查是否此檔案
            try 
            {
                fis = new FileInputStream(fileName);
            }
             catch (FileNotFoundException e) 
            {
                fileExists = false;
            }

            try{
                if(!fileExists)
                {
                    //判斷是否為帳號密碼
                    if(textName.contains("/"))
                    {
                        textExists=true;
                    }
                    else
                    {
                        textExists=false;
                    }
                }
            }
            catch(Exception e)
            {
                System.out.println(e);
            }
            //檢驗身分
            if (textExists)
            {
                List<String> Accountlist=new ArrayList<>();
                //文字切割，並將文字存放進list裡面
                for (String retval: textName.split("/")){
                    Accountlist.add(retval);
                } 
                //驗證帳號密碼是否正確
                if(!Accountlist.get(0).contains("oldjeff")||!Accountlist.get(1).contains("123456"))
                {   
                    statusLine = "HTTP/1.1 200 Ok "+ CRLF;
                    contentTypeLine = "Content-Type: text/html"+ CRLF;
                    entityBody = "<HTML>"+"<HEAD><TITLE>Denied</TITLE></HEAD>" +"<BODY><script type='text/javascript'>alert('Your name or password has mistake')</script></BODY></HTML>";
                }
                else
                {// 輸出URL後面字串
                System.out.println("Account:"+Accountlist.get(0)+",password: "+Accountlist.get(1));
                statusLine = "HTTP/1.1 200 Ok "+ CRLF;
                contentTypeLine = "Content-Type: text/html"+ CRLF;
                entityBody = "<HTML>"+"<HEAD><TITLE>Account</TITLE></HEAD>" +"<BODY>Login successful </BODY></HTML>";
                }
            
            }
            //檢驗檔案
            if(fileExists)
            {
                //建立回應訊息。
                String getFilename=contentType( fileName );
                //檢驗檔案是否正確
                if(getFilename!="application/octet-stream")
                {
                    statusLine = "HTTP/1.1 200 OK" + CRLF;
                    contentTypeLine = "Content-type: " + getFilename + CRLF;
                }
                else
                {
                    statusLine = "HTTP/1.1 404 Not Found"+ CRLF;
                    contentTypeLine = "Content-Type: text/html"+ CRLF;
                    entityBody = "<HTML>"+"<HEAD><TITLE>Denied</TITLE></HEAD>" +"<BODY><script type='text/javascript'>alert('Your request has been denied')</script></BODY></HTML>";
                }
            }
            //其餘輸入值
            else if(!textExists && !fileExists )
            {
                statusLine = "HTTP/1.1 404 Not Found"+ CRLF;
                contentTypeLine = "Content-Type: text/html"+ CRLF;
                entityBody = "<HTML>"+"<HEAD><TITLE>Denied</TITLE></HEAD>" +"<BODY><script type='text/javascript'>alert('Your request has been denied')</script></BODY></HTML>";
            }            

            //傳送狀態行
            os.writeBytes(statusLine);
            //傳送內容類型行
            os.writeBytes(contentTypeLine);
            //傳送空行表示標頭結束
            os.writeBytes(CRLF);
            try
            {
                if (fileExists)	
                {
                    sendBytes(fis, os);
                    fis.close();
                }
                else if(textExists)
                {
                    os.writeBytes(entityBody);
                }
                else 
                {
                    os.writeBytes(entityBody);
                }                
            }catch(Exception e)
            {
                System.out.println(e);
            }
            //關閉和socket
            os.close();
            br.close();
            socket.close();
        }
    }

        private static void sendBytes(FileInputStream fis, OutputStream os)throws Exception
    {
       //建立一個1K的緩衝區來存放要傳送至socket的位元組。
       byte[] buffer = new byte[1024];
       int bytes = 0;
    
       //將請求的檔案複製到 socket 的輸出串流中。
       while((bytes = fis.read(buffer)) != -1 ) 
       {
          os.write(buffer, 0, bytes);
       }
    }
    //根據名稱判斷內容類型(Content-Type)，並回傳相應的檔案類型
    private static String contentType(String fileName)
    {
        if(fileName.endsWith(".htm") || fileName.endsWith(".html")) 
        {
            return "text/html";
        }

        if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) 
        {
            return "image/jpeg";
        }

        if(fileName.endsWith(".png")) 
        {
            return "image/png";
        }

        if(fileName.endsWith(".gif"))
        {
            return "image/gif";
        }

        return "application/octet-stream";
    }

}