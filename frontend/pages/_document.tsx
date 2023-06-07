import { Html, Head, Main, NextScript } from 'next/document'
import {ThemeProvider} from "@primer/react-brand";

export default function Document() {
  return (
      <ThemeProvider>
        <Html lang="en">
          <Head />
          <body>
            <Main />
            <NextScript />
            <p style={{'textAlign': 'center', 'padding': '32px 0 16px 0'}}>©2023 Concise Video Summarizer</p>
          </body>
        </Html>
      </ThemeProvider>
  )
}
