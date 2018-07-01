#include<built_in.h>

unsigned char UART1_Readchar()
{
  unsigned char temp;
  while(UART1_Data_Ready()!=1)
   ;
  temp = UART1_Read();
  return temp;
}

void serial_communication()
{
 unsigned char cmd, temp, indata, dataout;
 int adc_rd;
 if(UART1_Data_Ready() != 1)
    return;

 cmd = UART1_Read();

  switch(cmd)
  {
   
  case 1 :
          DDRA = 0xff;
          PORTA = UART1_Readchar();
          break;
 case 2 :
          DDRB = 0xff;
          PORTB = UART1_Readchar();
          break;
 case 3 :
          DDRC = 0xff;
          PORTC = UART1_Readchar();
          break;
 case 4 :
          DDRD = 0xff;
          PORTD = UART1_Readchar();
          break;
 
case 5 :
          temp = UART1_Readchar();
          adc_rd = ADC_Read(temp);
          UART1_Write(adc_rd);
          break;

  }
}


void main() {
  unsigned char temp;
  UART1_Init(9600);              // Initialize UART module at 9600 bps
  Delay_ms(100);                 // Wait for UART module to stabilize

  while (1)
  {
   serial_communication();
  }
}