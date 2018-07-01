
_main:
	LDI        R27, 255
	OUT        SPL+0, R27
	LDI        R27, 0
	OUT        SPL+1, R27
	IN         R28, SPL+0
	IN         R29, SPL+1
	SBIW       R28, 6
	OUT        SPL+0, R28
	OUT        SPL+1, R29
	ADIW       R28, 1

;Avr.c,3 :: 		void main() {
;Avr.c,6 :: 		UART1_Init(9600);               // Initialize UART module at 9600 bps
	PUSH       R2
	LDI        R27, 51
	OUT        UBRRL+0, R27
	LDI        R27, 0
	OUT        UBRRH+0, R27
	CALL       _UART1_Init+0
;Avr.c,7 :: 		Delay_ms(100);                  // Wait for UART module to stabilize
	LDI        R18, lo_addr(R5)
	LDI        R17, lo_addr(R15)
	LDI        R16, 242
L_main0:
	DEC        R16
	BRNE       L_main0
	DEC        R17
	BRNE       L_main0
	DEC        R18
	BRNE       L_main0
;Avr.c,9 :: 		DDRC = 0xFF;               // Set PORTC as output
	LDI        R27, 255
	OUT        DDRC+0, R27
;Avr.c,10 :: 		PORTC = 0;
	LDI        R27, 0
	OUT        PORTC+0, R27
;Avr.c,11 :: 		while(1){
L_main2:
;Avr.c,12 :: 		adc_rd = ADC_Read(0);    // get ADC value from 0nd channel
	CLR        R2
	CALL       _ADC_Read+0
	STS        _adc_rd+0, R16
	STS        _adc_rd+1, R17
;Avr.c,13 :: 		adc_rd = (adc_rd >> 2) & 0xFF;
	LSR        R17
	ROR        R16
	LSR        R17
	ROR        R16
	ANDI       R16, 255
	ANDI       R17, 0
	STS        _adc_rd+0, R16
	STS        _adc_rd+1, R17
;Avr.c,15 :: 		if (((adc_rd * 2)-2) > 50){
	LSL        R16
	ROL        R17
	MOVW       R18, R16
	SUBI       R18, 2
	SBCI       R19, 0
	LDI        R16, 50
	LDI        R17, 0
	CP         R16, R18
	CPC        R17, R19
	BRLO       L__main9
	JMP        L_main4
L__main9:
;Avr.c,16 :: 		PORTC = 1;
	LDI        R27, 1
	OUT        PORTC+0, R27
;Avr.c,17 :: 		}else{
	JMP        L_main5
L_main4:
;Avr.c,18 :: 		PORTC = 0;
	LDI        R27, 0
	OUT        PORTC+0, R27
;Avr.c,19 :: 		}
L_main5:
;Avr.c,21 :: 		adc_rd = (adc_rd * 2)-2 ;
	LDS        R16, _adc_rd+0
	LDS        R17, _adc_rd+1
	LSL        R16
	ROL        R17
	SUBI       R16, 2
	SBCI       R17, 0
	STS        _adc_rd+0, R16
	STS        _adc_rd+1, R17
;Avr.c,22 :: 		temp1 = (adc_rd / 100) + 48;
	LDI        R20, 100
	LDI        R21, 0
	CALL       _Div_16x16_U+0
	MOVW       R16, R24
	SUBI       R16, 208
	SBCI       R17, 255
	STD        Y+0, R16
	STD        Y+1, R17
;Avr.c,23 :: 		temp4 = adc_rd % 100;
	LDI        R20, 100
	LDI        R21, 0
	LDS        R16, _adc_rd+0
	LDS        R17, _adc_rd+1
	CALL       _Div_16x16_U+0
	MOVW       R16, R26
	STD        Y+4, R16
	STD        Y+5, R17
;Avr.c,24 :: 		temp2 = (temp4 / 10) + 48;
	LDI        R20, 10
	LDI        R21, 0
	CALL       _Div_16x16_S+0
	MOVW       R16, R22
	SUBI       R16, 208
	SBCI       R17, 255
	STD        Y+2, R16
	STD        Y+3, R17
;Avr.c,25 :: 		temp3 = (temp4 % 10) + 48;
	LDI        R20, 10
	LDI        R21, 0
	LDD        R16, Y+4
	LDD        R17, Y+5
	CALL       _Div_16x16_S+0
	MOVW       R16, R24
	SUBI       R16, 208
	SBCI       R17, 255
; temp3 start address is: 16 (R16)
;Avr.c,27 :: 		UART1_Write('T');
	LDI        R27, 84
	MOV        R2, R27
	CALL       _UART1_Write+0
;Avr.c,28 :: 		UART1_Write('E');
	LDI        R27, 69
	MOV        R2, R27
	CALL       _UART1_Write+0
;Avr.c,29 :: 		UART1_Write('M');
	LDI        R27, 77
	MOV        R2, R27
	CALL       _UART1_Write+0
;Avr.c,30 :: 		UART1_Write('P');
	LDI        R27, 80
	MOV        R2, R27
	CALL       _UART1_Write+0
;Avr.c,31 :: 		UART1_Write(':');
	LDI        R27, 58
	MOV        R2, R27
	CALL       _UART1_Write+0
;Avr.c,32 :: 		UART1_Write(' ');
	LDI        R27, 32
	MOV        R2, R27
	CALL       _UART1_Write+0
;Avr.c,33 :: 		UART1_Write(temp1);
	LDD        R2, Y+0
	CALL       _UART1_Write+0
;Avr.c,34 :: 		UART1_Write(temp2);
	LDD        R2, Y+2
	CALL       _UART1_Write+0
;Avr.c,35 :: 		UART1_Write(temp3);
	MOV        R2, R16
; temp3 end address is: 16 (R16)
	CALL       _UART1_Write+0
;Avr.c,36 :: 		UART1_Write(' ');
	LDI        R27, 32
	MOV        R2, R27
	CALL       _UART1_Write+0
;Avr.c,37 :: 		UART1_Write(248);
	LDI        R27, 248
	MOV        R2, R27
	CALL       _UART1_Write+0
;Avr.c,38 :: 		UART1_Write('C');
	LDI        R27, 67
	MOV        R2, R27
	CALL       _UART1_Write+0
;Avr.c,39 :: 		UART1_Write(13);
	LDI        R27, 13
	MOV        R2, R27
	CALL       _UART1_Write+0
;Avr.c,40 :: 		UART1_Write(10);
	LDI        R27, 10
	MOV        R2, R27
	CALL       _UART1_Write+0
;Avr.c,41 :: 		Delay_ms(10);
	LDI        R17, 104
	LDI        R16, 229
L_main6:
	DEC        R16
	BRNE       L_main6
	DEC        R17
	BRNE       L_main6
;Avr.c,42 :: 		}
	JMP        L_main2
;Avr.c,43 :: 		}
L_end_main:
	JMP        L_end_main
; end of _main
