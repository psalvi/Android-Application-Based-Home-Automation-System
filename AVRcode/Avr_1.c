#include<built_in.h>
 unsigned int adc_rd;
void main() {

    int temp1,temp2,temp3,temp4,temp5,temp6;
     UART1_Init(9600);               // Initialize UART module at 9600 bps
     Delay_ms(100);                  // Wait for UART module to stabilize

    DDRC = 0xFF;               // Set PORTC as output
    PORTC = 0;
    while(1){
          adc_rd = ADC_Read(0);    // get ADC value from 0nd channel
          adc_rd = (adc_rd >> 2) & 0xFF;
          
          if (((adc_rd * 2)-2) > 50){
             PORTC = 1;
          }else{
             PORTC = 0;
          }
          
         adc_rd = (adc_rd * 2)-2 ;
         temp1 = (adc_rd / 100) + 48;
         temp4 = adc_rd % 100;
         temp2 = (temp4 / 10) + 48;
         temp3 = (temp4 % 10) + 48;
         
         UART1_Write('T');
         UART1_Write('E');
         UART1_Write('M');
         UART1_Write('P');
         UART1_Write(':');
         UART1_Write(' ');
         UART1_Write(temp1);
         UART1_Write(temp2);
         UART1_Write(temp3);
         UART1_Write(' ');
         UART1_Write(248);
         UART1_Write('C');
         UART1_Write(13);
         UART1_Write(10);
         Delay_ms(10);
    }
}