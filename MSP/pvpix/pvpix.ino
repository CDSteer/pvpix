#include <Servo.h>
 
Servo myservo0;
Servo myservo1;
Servo myservo2;
Servo myservo3;
         

int open_close0, open_close1, open_close2, open_close3, wakeBtn = 0;
int sw0, sw1, sw2, sw3 = 0;

bool readMode = false;

bool ble = true;
int loopCount = 0;

int writeToBLE(String mesg){
  delay(100);                                         //Oberve Vdd0, Vdd1 etc with oscilloscope. Increase delay accordingly.
  Serial1.println(mesg);
  delay(100);
}

int* splitCommand(String text, char splitChar) {
  int sa[4], r=0, t=0;
  String oneLine = text;
  Serial.println(oneLine);
  if (oneLine.length()>4){
    loopCount = 0;
    Serial.print("Incoming States: ");
    for (int i=0; i < oneLine.length(); i++) { 
      if(oneLine.charAt(i) == ':') {
        sa[t] = oneLine.substring(r, i).toInt();
        Serial.println(sa[t]);
        r=(i+1); 
        t++;
      }
      if (open_close0 != sa[0]){
        sw0 =  1;
        open_close0 = sa[0];
      }
      
      if (open_close1 != sa[1]) {
        sw1 = 1;
        open_close1 = sa[1];
      }
      
      if (open_close2 != sa[2]) {
        sw2 = 1;
        open_close2 = sa[2];
      }
      
      if (open_close3 != sa[3]) {
        sw3 = 1;
        open_close3 = sa[3];
      }
    }
  }
  
  readMode = true;
  return sa;
}



void setup()
{
 

  P1DIR |= BIT0+BIT1+BIT2+BIT3+BIT4+BIT5+BIT6+BIT7;
  P1OUT &= (~BIT0)+(~BIT1)+(~BIT2)+(~BIT3)+(~BIT4)+(~BIT5)+(~BIT6)+(~BIT7);
  P2DIR |= BIT0+BIT1+BIT2+BIT3+BIT4+BIT5+BIT6+BIT7;
  P2OUT &= (~BIT0)+(~BIT1)+(~BIT2)+(~BIT3)+(~BIT4)+(~BIT5)+(~BIT6)+(~BIT7);
  P3DIR |= BIT0+BIT1+BIT2+BIT3+BIT4+BIT5+BIT6+BIT7;
  P3OUT &= (~BIT0)+(~BIT1)+(~BIT2)+(~BIT3)+(~BIT4)+(~BIT5)+(~BIT6)+(~BIT7);
  P4DIR |= BIT0+BIT1+BIT2+BIT3+BIT4+BIT5+BIT6+BIT7;
  P4OUT &= (~BIT0)+(~BIT1)+(~BIT2)+(~BIT3)+(~BIT4)+(~BIT5)+(~BIT6)+(~BIT7);
  P5DIR |= BIT0+BIT1+BIT2+BIT3+BIT4+BIT5+BIT6+BIT7;
  P5OUT &= (~BIT0)+(~BIT1)+(~BIT2)+(~BIT3)+(~BIT4)+(~BIT5)+(~BIT6)+(~BIT7);
  P6DIR |= BIT0+BIT1+BIT2+BIT3+BIT4+BIT5+BIT6+BIT7;
  P6OUT &= (~BIT0)+(~BIT1)+(~BIT2)+(~BIT3)+(~BIT4)+(~BIT5)+(~BIT6)+(~BIT7);
  P7DIR |= BIT0+BIT1+BIT2+BIT3+BIT4+BIT5+BIT6+BIT7;
  P7OUT &= (~BIT0)+(~BIT1)+(~BIT2)+(~BIT3)+(~BIT4)+(~BIT5)+(~BIT6)+(~BIT7);
  P8DIR |= BIT0+BIT1+BIT2;
  P8OUT &= (~BIT0)+(~BIT1)+(~BIT2);
 
 
  Serial.begin(9600);           //UART to virtual COM for debug
  Serial1.begin(9600);          

  pinMode(P4_3, OUTPUT); //IN1
  pinMode(P4_0, OUTPUT); //IN2
  pinMode(P3_7, OUTPUT); //IN1
  pinMode(P8_2, OUTPUT); //IN2

  pinMode(P8_1, OUTPUT); //BLE enable
       
  pinMode(P2_2, INPUT_PULLUP); //SW0
  pinMode(P2_0, INPUT_PULLUP); //SW1
  pinMode(P2_6, INPUT_PULLUP); //SW2
  pinMode(P2_3, INPUT_PULLUP); //SW3

  pinMode(P2_1, INPUT_PULLUP); //wakeBtn

 
  myservo0.attach(P2_5); //Sig_0
  myservo1.attach(P2_4); //Sig_0
  myservo2.attach(P1_5); //Sig_0
  myservo3.attach(P1_4); //Sig_0


  attachInterrupt(digitalPinToInterrupt(P2_2), toggle0, FALLING);
  attachInterrupt(digitalPinToInterrupt(P2_0), toggle1, FALLING);
  attachInterrupt(digitalPinToInterrupt(P2_6), toggle2, FALLING);
  attachInterrupt(digitalPinToInterrupt(P2_3), toggle3, FALLING);

  attachInterrupt(digitalPinToInterrupt(P2_1), toggleRst, FALLING);

  //Initialising
  Serial1.println("I'M ALIVE!");
  delay(100);
//  digitalWrite(P8_1, LOW); //Remove power to BLE
  digitalWrite(P8_1, HIGH); //Power to BLE
  delay(100);
  digitalWrite(P4_3, HIGH); //Power to SERVO_0
  delay(100);
  digitalWrite(P4_0, HIGH); //Power to SERVO_1
  delay(100);
  digitalWrite(P3_7, HIGH); //Power to SERVO_2
  delay(100);
  digitalWrite(P8_2, HIGH); //Power to SERVO_3
  delay(500);
  myservo0.attach(P2_5);
  delay(100);
  myservo1.attach(P2_4);
  delay(100);
  myservo2.attach(P1_5);
  delay(100);
  myservo3.attach(P1_4);
  delay(500);
  myservo0.detach();
  myservo1.detach();
  myservo2.detach();
  myservo3.detach();
  delay(500);
  digitalWrite(P4_3, LOW);
  digitalWrite(P4_0, LOW);
  digitalWrite(P3_7, LOW);
  digitalWrite(P8_2, LOW);
 
  Serial.println("Initialised");

}

int* states;
String sendMesg = "";
void loop() {
   // read in a new pv message
   Serial1.begin(9600);  
//   Serial.println(loopCount);
   
   loopCount++;
  
    if (loopCount>20) {
      digitalWrite(P8_1, LOW); //Remove power to BLE
      sendMesg = "";
      wakeBtn = 0;
      suspend();
    } else {
      digitalWrite(P8_1, HIGH); //Power to BLE
      writeToBLE(sendMesg);
      if (Serial1.available() > 0) {
        String data_received = Serial1.readStringUntil(';');
        Serial.print("datsa");
        Serial.println(data_received);
        if (data_received) {
          states = splitCommand(data_received, ';'); 
        }     
      }
    }
  
   delay(900);

   if(sw0 == 1){
      loopCount = 0;
      digitalWrite(P4_3, HIGH); //Power to SERVO_0
      delay(100);
      digitalWrite(P8_1, HIGH); //Power to BLE
      delay(700);                                         //Oberve Vdd0, Vdd1 etc with oscilloscope. Increase delay accordingly.
      myservo0.attach(P2_5);
      delay(300); 
      if(open_close0 == 1){
        if (!readMode) {
          sendMesg = "1:0";
          delay(100);
          myservo0.write(90);
          delay(500);
        } else {
          sendMesg = "1:1";
          delay(100);
          myservo0.write(0);
          delay(500);
        }
      } else {
        if (!readMode) {
          sendMesg = "1:1";
          delay(100);
          myservo0.write(0);
          delay(500);
        } else {
          sendMesg = "1:0";
          delay(100);
          myservo0.write(90);
          delay(500);
        }
      }
      delay(100);
      sw0 = 0;
    }
   if(sw1 == 1){
    loopCount = 0;
      digitalWrite(P4_0, HIGH); //Power to SERVO_1
      delay(100);
//      digitalWrite(P8_1, HIGH); //Power to BLE
      delay(700);
      myservo1.attach(P2_4);
      delay(300);
     
      if(open_close1 == 1){    
        if (!readMode) {
          
//          Serial1.println("1:0");
          delay(100);
          myservo1.write(90);
          delay(500);
          sendMesg = "0:0";
        } else {
//          Serial.println("1:1");
          delay(100);
          myservo1.write(0);
          delay(500);
          sendMesg = "0:1";
        }
      } else {
        if (!readMode) {
//          Serial1.println("1:1");
          delay(100);
          myservo1.write(0);
          delay(500);
          sendMesg = "0:1";
        } else {
//          Serial.println("1:0");
          delay(100);
          myservo1.write(90);
          delay(500);
          sendMesg = "0:0";
        }
      }
      delay(100);
      sw1 = 0;

    }
   if(sw2 == 1){
    loopCount = 0;
      digitalWrite(P3_7, HIGH); //Power to SERVO_2
      delay(100);
      digitalWrite(P8_1, HIGH); //Power to BLE
      delay(700);
      myservo2.attach(P1_5);
      delay(300);
      if(open_close2 == 1){    
        if (!readMode) {
          sendMesg ="3:0";
          delay(100);
          myservo2.write(90);
          delay(500);
        } else {
          delay(100);
          myservo2.write(0);
          delay(500);
        }
      } else {
        if (!readMode) {
          sendMesg = "3:1";
          delay(100);
          myservo2.write(0);
          delay(500);
        } else {
          delay(100);
          myservo2.write(90);
          delay(500);
        }
      }
      delay(1000);
      sw2 = 0;
    }
    if(sw3 == 1){
      loopCount = 0;
      digitalWrite(P8_2, HIGH); //Power to SERVO_3
      delay(100);
      digitalWrite(P8_1, HIGH); //Power to BLE
      delay(700);
      myservo3.attach(P1_4);
      delay(300);
      
      if(open_close3 == 1){    
        if (!readMode) {
          sendMesg = "2:0";
          delay(100);
          myservo3.write(90);
          delay(500);
        } else {
          delay(100);
          myservo3.write(0);
          delay(500);
        }
      } else {
        if (!readMode) {
          sendMesg = "2:1";
          delay(100);
          myservo3.write(0);
          delay(500);
        } else {
          delay(100);
          myservo3.write(90);
          delay(500);
        }
      }
      delay(100);
      sw3 = 0;
    }
     
  delay(30);

  myservo0.detach();
  myservo1.detach();
  myservo2.detach();
  myservo3.detach();
     


 
  pinMode(P2_5, OUTPUT); //Servo PWM pin to output
  pinMode(P2_4, OUTPUT); //Servo PWM pin to output
  pinMode(P1_5, OUTPUT); //Servo PWM pin to output
  pinMode(P1_4, OUTPUT); //Servo PWM pin to output
 
  pinMode(P3_3, INPUT_PULLDOWN); //BLE -> GPIO input_pulldown
  pinMode(P3_4, INPUT_PULLDOWN); //BLE -> GPIO " "

 
  digitalWrite(P4_3, LOW);
  digitalWrite(P4_0, LOW);
  digitalWrite(P3_7, LOW);
  digitalWrite(P8_2, LOW);

  delay(30);
  
  readMode = false;
}

 //******************* ISR *******************//
void toggle0(){
  wakeup();
  open_close0 = !open_close0;
  sw0 = 1;
}

void toggle1(){
  wakeup();
  open_close1 = !open_close1;
  sw1 = 1;
}


void toggle2(){
  wakeup();
  open_close2 = !open_close2;
  sw2 = 1;
}

void toggle3(){
  wakeup();
  open_close3 = !open_close3;
  sw3 = 1;
}

void toggleRst(){
  wakeup();
  loopCount = 0;
  wakeBtn = 1;
}
