#include<built_in.h>


#define TXB8 0
#define RXB8 1
#define UPE  2
#define OVR  3
#define FE   4
#define UDRE 5
#define RXC  7

#define FRAMING_ERROR (1<<FE)
#define PARITY_ERROR (1<<UPE)
#define DATA_OVERRUN (1<<OVR)
#define DATA_REGISTER_EMPTY (1<<UDRE)


unsigned char FlagReg;
sbit ZC at FlagReg.B0;
unsigned char Count;

//************************* UART Functions ************************
void USART_Write( unsigned char ch ) {
    /* Wait for empty transmit buffer */
    while ( !( UCSRA & (1<<UDRE)) ); /* Put data into buffer, sends the data */
    UDR = ch;

}
void USART_Init(void ) {
    /* Set baud rate
    UBRRH = (unsigned char)(baud>>8);
    UBRRL = (unsigned char)baud;*/
// 9600-Baud Rate initialization(0x33 for 8MHz,0x40 for 10MHz,0x67 for 16MHz )
        UBRRH = 0x00;
        UBRRL = 0x33;

    /* Enable receiver and transmitter */
    UCSRB = (1<<RXEN)|(1<<TXEN);
    UCSRC = (1<<URSEL)|(0<<USBS)|(3<<UCSZ0);  // Set frame format: 8data, 1stop bit
    UCSRB |= (1 << RXCIE); // Enable the USART Recieve Complete interrupt (USART_RXC)
    asm sei// Enable the Global Interrupt Enable flag so that interrupts can be processed
}
unsigned char USART_Receive( void ) {
    /* Wait for data to be received */
    while ( !(UCSRA & (1<<RXC)) ); /* Get and return received data from buffer */
    return UDR;
}
// ***********************************************************

void serial_ISR() {
    unsigned char status;//, cmd;
    unsigned char cmd, indata, dataout;
    int adc_rd,temp;

    status=UCSRA;
    cmd=UDR;

    if ((status & (FRAMING_ERROR | PARITY_ERROR | DATA_OVERRUN))==0) {
    
    switch(cmd)
  {
   
   case 34 :
          DDRB = 0x00;
          PORTB = USART_Receive();
          break;
   case 35 :
          DDRC = 0x00;
          PORTC = USART_Receive();
          break;
   case 72 :
          DDRD = 0x00;
          dataout = PIND;
          USART_Write(dataout);
          break;
   case 73 :
          temp = USART_Receive();

          adc_rd = ADC_Read(temp);    // get ADC value from 2nd channel
          adc_rd = adc_rd >> 2;
          temp = adc_rd & 0xff;
          USART_Write(temp);
          break;
   case 74 :
          Count = USART_Receive();
          break;
  }
 }
}


void interrupt() org IVT_ADDR_INT0{
     ZC = 1;
}
void main() {
  unsigned char temp;
  unsigned char i;
  ISC01_bit = 1;             //External interrupt on falling edge
  ISC00_bit = 0;
  INT0_bit = 1;              //Enable ext int 0
  INT1_bit = 0;              //Disable ext int 1
  INT2_bit = 0;              //Disable ext int 2
  SREG_I_bit = 1;            //Enable interrupts
  
  USART_Init();              // Initialize UART module at 9600 bps
  Delay_ms(100);                 // Wait for UART module to stabilize

   ADC_Init();
   DDRA = 0x00;
   DDRB = 0xff;
   DDRC = 0xff;
   Delay_ms(200);
   PORTB = 0;
   PORTC = 0;
   Count = 100;
  while (1)
  {
          //PORTD = ZC;
          if (ZC){ //zero crossing occurred
              for (i = 0; i < Count; i++){
                  Delay_us(500);
              }
              PORTC.B0 = 1; //Send a pulse
              Delay_us(150);
              PORTC.B0 = 0;

              ZC = 0;
           }
  }
}