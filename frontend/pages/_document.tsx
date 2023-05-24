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
          </body>
        </Html>
      </ThemeProvider>
  )
}
